import { Component, Input, OnChanges, SimpleChanges, ElementRef, AfterViewInit, OnDestroy, ViewChild, Renderer2, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PropertyListingDTO } from '../../../../../src/app/models/property-listing.dto';
import { Property } from '../../models/property.model';
import { Institute } from '../../../../../src/app/models/institute.model';

// OpenLayers imports
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';
import { fromLonLat } from 'ol/proj';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Style from 'ol/style/Style';
import Fill from 'ol/style/Fill';
import Stroke from 'ol/style/Stroke';
import CircleStyle from 'ol/style/Circle';
import Icon from 'ol/style/Icon';
import { FeatureLike } from "ol/Feature";
import Overlay from 'ol/Overlay';
import Text from 'ol/style/Text';

// Clustering
import Cluster from 'ol/source/Cluster';
import { boundingExtent } from 'ol/extent';

@Component({
  selector: 'app-map-display',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './map-display.component.html',
  styleUrls: ['./map-display.component.css']
})
export class MapDisplayComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() properties: (PropertyListingDTO | Property)[] = [];
  @Input() selectedInstitute: Institute | null = null;
  @Input() visiblePropertyIds: number[] = [];
  @Input() selectedPropertyId: number | null = null;

  @Output() propertySelected = new EventEmitter<number>();

  @ViewChild('popupContainer') popupContainerRef!: ElementRef<HTMLDivElement>;
  @ViewChild('popupContent') popupContentRef!: ElementRef<HTMLDivElement>;

  private map: Map | null = null;
  mapId = 'ol-map-' + Math.random().toString(36).substring(2);
  private vectorSource: VectorSource<Feature<Point>>;
  private clusterSource: Cluster;
  private vectorLayer: VectorLayer<VectorSource<Feature<Point>> | Cluster> | null = null;
  private instituteVectorSource: VectorSource<Feature<Point>>;
  private popupOverlay!: Overlay;
  isPopupVisible = false;
  private resizeObserver: ResizeObserver | null = null;
  
  private isInstituteSpecificView = false;
  private defaultCenter = fromLonLat([10.1815, 36.8065]);
  private defaultZoom = 7;

  private propertyTypeColors: { [key: string]: string } = {
    'apartment': 'rgba(255, 87, 34, 0.8)',
    'house': 'rgba(76, 175, 80, 0.8)',
    'studio': 'rgba(33, 150, 243, 0.8)',
    'villa': 'rgba(156, 39, 176, 0.8)',
    'room': 'rgba(255, 193, 7, 0.8)',
    'default': 'rgba(255, 0, 0, 0.8)'
  };

  activePopupImages: string[] = [];
  currentPopupImageIndex: number = 0;
  activePopupPropertyId: number | null = null;
  activePopupPropertySourceType: string | null = null;

  // Add style cache for better performance
  private styleCache: {[key: string]: Style} = {};
  private clusterStyleCache: {[key: string]: Style} = {};

  constructor(private elementRef: ElementRef, private renderer: Renderer2) {
    this.vectorSource = new VectorSource({
      wrapX: true
    });
    this.clusterSource = new Cluster({
      distance: 0, 
      minDistance: 0, 
      source: this.vectorSource,
      geometryFunction: (feature) => {
        const geom = feature.getGeometry();
        return geom === undefined ? null : geom;
      }
    });
    this.instituteVectorSource = new VectorSource({
      wrapX: true
    });
  }

  // --- Type Guard and Helper Methods ---
  private getNumericId(prop: PropertyListingDTO | Property): number {
    const id = prop.id;
    // Ensure that if id is a string, it's a valid number before parsing
    if (typeof id === 'string') {
        const parsedId = parseInt(id, 10);
        return isNaN(parsedId) ? -1 : parsedId;
    }
    return id || -1;
  }

  private cleanImageUrl(url: string | undefined | null): string | undefined {
    if (!url) return undefined;
    return url.startsWith('@') ? url.substring(1) : url;
  }

  private getLatitude(prop: PropertyListingDTO | Property): number | undefined {
    if ('latitude' in prop && typeof prop.latitude === 'number') {
      return prop.latitude;
    }
    if ('location' in prop && typeof prop.location === 'object' && prop.location && 'lat' in prop.location && typeof prop.location.lat === 'number') {
      return prop.location.lat;
    }
    return undefined;
  }

  private getLongitude(prop: PropertyListingDTO | Property): number | undefined {
    if ('longitude' in prop && typeof prop.longitude === 'number') {
      return prop.longitude;
    }
    if ('location' in prop && typeof prop.location === 'object' && prop.location && 'lng' in prop.location && typeof prop.location.lng === 'number') {
      return prop.location.lng;
    }
    return undefined;
  }
  
  private getMainImageUrl(prop: Property | PropertyListingDTO): string | undefined {
    let mainImgUrl: string | undefined;
    if ('mainImageUrl' in prop && prop.mainImageUrl) mainImgUrl = this.cleanImageUrl(prop.mainImageUrl);
    if (!mainImgUrl && 'imageUrl' in prop && prop.imageUrl) mainImgUrl = this.cleanImageUrl(prop.imageUrl);
    if (!mainImgUrl) {
        const images = this.getImageArray(prop);
        if (images && images.length > 0) mainImgUrl = images[0]; // Already cleaned by getImageArray
    }
    return mainImgUrl;
  }

  private getImageArray(prop: Property | PropertyListingDTO): string[] | undefined {
    let cleanedImageUrls: string[] | undefined;
    if ('imageUrls' in prop && Array.isArray(prop.imageUrls)) {
      cleanedImageUrls = prop.imageUrls.map(url => this.cleanImageUrl(url)).filter(Boolean) as string[];
    }
    if ((!cleanedImageUrls || cleanedImageUrls.length === 0) && 'images' in prop && Array.isArray(prop.images)) {
      cleanedImageUrls = prop.images.map(url => this.cleanImageUrl(url)).filter(Boolean) as string[];
    }
    return cleanedImageUrls && cleanedImageUrls.length > 0 ? cleanedImageUrls : undefined;
  }

  private getPropertyAddress(prop: PropertyListingDTO | Property): string {
    if ('address' in prop && typeof prop.address === 'string' && prop.address) {
        return prop.address;
    }
    if ('location' in prop && typeof prop.location === 'string' && prop.location) { // PropertyListingDTO.location might be address
        return prop.location;
    }
    if ('city' in prop && prop.city) { // Fallback for PropertyListingDTO
        return prop.city + (('district' in prop && prop.district) ? `, ${prop.district}` : '');
    }
    return 'Address not available';
  }

  private getPropertyType(prop: PropertyListingDTO | Property): string {
    return prop.propertyType || 'Property';
  }
  
  private getPrice(prop: PropertyListingDTO | Property): string {
    const price = prop.price;
    if (typeof price !== 'number') return 'Price not available';
    const currency = ('currency' in prop && typeof prop.currency === 'string') ? prop.currency : 'TND';
    return `${price} ${currency}/month`;
  }

  private getBedrooms(prop: PropertyListingDTO | Property): number | undefined {
    if ('bedrooms' in prop && typeof prop.bedrooms === 'number') return prop.bedrooms;
    if ('beds' in prop && typeof prop.beds === 'number') return prop.beds;
    return undefined;
  }

  private getBathrooms(prop: PropertyListingDTO | Property): number | undefined {
     if ('bathrooms' in prop && typeof prop.bathrooms === 'number') return prop.bathrooms;
     if ('baths' in prop && typeof prop.baths === 'number') return prop.baths;
     return undefined;
  }
  // --- End Helper Methods ---

  ngAfterViewInit(): void {
    const mapContainer = this.elementRef.nativeElement.querySelector('.map-container');
    if (mapContainer) {
      this.renderer.setStyle(mapContainer, 'width', '100%');
      this.renderer.setStyle(mapContainer, 'height', '500px'); 
      this.renderer.setStyle(mapContainer, 'min-height', '500px');
      
      this.resizeObserver = new ResizeObserver(entries => {
        const { width, height } = entries[0].contentRect;
        if (width > 0 && height > 0) {
          if (!this.map) {
            this.initMap();
          } else {
            setTimeout(() => this.map?.updateSize(), 100);
          }
        }
      });
      this.resizeObserver.observe(mapContainer);
      setTimeout(() => { if (!this.map) this.initMap(); }, 1000);
    } else {
      console.error('Map container not found for MapDisplayComponent!');
      setTimeout(() => { if (!this.map) this.initMap(); }, 300);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.map) {
      if (changes['selectedPropertyId'] && !changes['properties']) {
        this.highlightSelectedProperty();
      }
      if (changes['visiblePropertyIds'] && !changes['properties']) {
        this.updateVisibleProperties();
      }
      if (changes['selectedInstitute']) {
        const oldInstitute = changes['selectedInstitute'].previousValue;
        this.isInstituteSpecificView = !!changes['selectedInstitute'].currentValue;
        if (this.isInstituteSpecificView !== !!oldInstitute) {
          this.updateClusterDistance();
        }
      }
      if (changes['properties'] || changes['selectedInstitute']) {
         this.updateMapFeatures();
      }
    }
  }

  private initMap(): void {
    if (this.map || !this.popupContainerRef?.nativeElement) {
        console.warn('Map already initialized or popup container not ready.');
        return;
    }
    this.vectorLayer = new VectorLayer({
      source: this.isInstituteSpecificView ? this.vectorSource : this.clusterSource,
      updateWhileAnimating: true,
      updateWhileInteracting: true,
      style: (featureLike: FeatureLike, resolution: number) => {
        const feature = featureLike as Feature<Point>;
        if (!feature.getGeometry() || !(feature.getGeometry() instanceof Point)) {
          return undefined; 
        }
        if (this.isInstituteSpecificView || !feature.get('features') || feature.get('features').length === 1) {
          const actualFeature = this.isInstituteSpecificView ? feature : (feature.get('features')[0] as Feature<Point>);
          return this.getPropertyStyle(actualFeature);
        } else {
          return this.getClusterStyle(feature, feature.get('features').length);
        }
      }
    });

    const instituteVectorLayer = new VectorLayer({
        source: this.instituteVectorSource,
        style: (featureLike: FeatureLike, resolution: number) => {
            const feature = featureLike as Feature<Point>;
            if (!feature.getGeometry() || !(feature.getGeometry() instanceof Point)) {
              return undefined; 
            }
            return this.getInstituteStyle(feature);
        }
    });

    this.popupOverlay = new Overlay({
      element: this.popupContainerRef.nativeElement,
      autoPan: {
        animation: {
          duration: 200
        },
        margin: 40  // Increased margin
      },
      offset: [0, 15],
      positioning: 'top-center',
      stopEvent: true
    });

    this.map = new Map({
      target: this.mapId,
      layers: [ new TileLayer({ source: new OSM() }), this.vectorLayer, instituteVectorLayer ],
      overlays: [this.popupOverlay],
      view: new View({ center: this.defaultCenter, zoom: this.defaultZoom, constrainResolution: true })
    });

    this.map.getView().on('change:resolution', () => {
      if (!this.isInstituteSpecificView) this.updateClusterDistance();
    });

    this.map.on('click', (event) => {
      let clickedFeature: Feature<Point> | undefined;
      this.map!.forEachFeatureAtPixel(event.pixel, (featureAtPixel, layer) => {
        if (layer === this.vectorLayer) {
          clickedFeature = featureAtPixel as Feature<Point>;
          return true; 
        }
        return false;
      });

      if (clickedFeature) {
        event.preventDefault();
        event.stopPropagation();
        
        const featuresInCluster = clickedFeature.get('features');
        if (this.isInstituteSpecificView || !featuresInCluster || featuresInCluster.length === 0) {
          const propertyData = clickedFeature.get('data') as (Property | PropertyListingDTO);
          if (!propertyData || propertyData.id === undefined || propertyData.id === null) return;
          const geometry = clickedFeature.getGeometry();
          if (geometry) this.handlePropertyClick(this.getNumericId(propertyData), geometry.getCoordinates());
        } else {
          if (featuresInCluster.length === 1) {
            const propertyFeature = featuresInCluster[0] as Feature<Point>;
            const propertyData = propertyFeature.get('data') as (Property | PropertyListingDTO);
            if (!propertyData || propertyData.id === undefined || propertyData.id === null) return;
            const geometry = propertyFeature.getGeometry();
            if (geometry) this.handlePropertyClick(this.getNumericId(propertyData), geometry.getCoordinates());
          } else if (featuresInCluster.length <= 5) {
            const primaryFeature = featuresInCluster[0] as Feature<Point>;
            const propertyData = primaryFeature.get('data') as (Property | PropertyListingDTO);
            const geometry = primaryFeature.getGeometry();
            if (geometry && propertyData) this.displayClusterPopup(propertyData, featuresInCluster as Feature<Point>[], geometry.getCoordinates());
          } else {
            const coordinates: number[][] = [];
            featuresInCluster.forEach((f: FeatureLike) => {
              const geom = (f as Feature<Point>).getGeometry();
              if (geom) coordinates.push(geom.getCoordinates());
            });
            if (coordinates.length > 0) {
              this.map!.getView().fit(boundingExtent(coordinates), { padding: [50, 50, 50, 50], duration: 800, maxZoom: 16 });
            }
          }
        }
      } else {
        this.closePopup();
      }
    });

    this.map.on('moveend', () => {
      if (this.vectorLayer) {
        this.vectorLayer.changed();
      }
    });

    this.updateMapFeatures();
  }

  public updateMapSize(): void { this.map?.updateSize(); }

  private updateClusterDistance() {
    if (!this.map || !this.clusterSource || !this.vectorLayer) return;

    const currentLayerSource = this.vectorLayer.getSource();
    if (this.isInstituteSpecificView) {
      if (currentLayerSource !== this.vectorSource) {
        this.vectorLayer.setSource(this.vectorSource);
      }
      return;
    } else {
      if (currentLayerSource !== this.clusterSource) {
        this.vectorLayer.setSource(this.clusterSource);
      }
    }

    const zoom = this.map.getView().getZoom() || 0;
    let newDistance = 0;
    if (this.properties.length > 100 && zoom < 14) newDistance = Math.max(5, 40 - (zoom * 3)); 
    else if (this.properties.length > 50 && zoom < 15) newDistance = Math.max(5, 25 - (zoom * 2)); 
    else newDistance = (this.properties.length > 20 && zoom < 16) ? 5 : 0;
    
    if (this.clusterSource.getDistance() !== newDistance) {
      this.clusterSource.setDistance(newDistance);
    }
  }

  // Optimized cluster style function with caching
  private getClusterStyle(feature: Feature<Point>, size: number): Style {
    // Use cache for common cluster sizes
    const cacheKey = `cluster-${size}`;
    if (this.clusterStyleCache[cacheKey]) {
      return this.clusterStyleCache[cacheKey];
    }
    
    const radius = Math.min(Math.max(15, Math.log2(size) * 10), 30);
    const svgSize = radius * 2;
    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="${svgSize}" height="${svgSize}" viewBox="0 0 ${svgSize} ${svgSize}"><circle cx="${svgSize/2}" cy="${svgSize/2}" r="${svgSize/2 - 1}" fill="#ff385c" stroke="white" stroke-width="2"/><text x="50%" y="53%" text-anchor="middle" dy=".3em" font-family="Arial" font-size="${Math.max(12, radius * 0.7)}" font-weight="bold" fill="white">${size}</text></svg>`;
    
    const style = new Style({ 
      image: new Icon({ src: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svgIcon), scale: 1, anchor: [0.5, 0.5] }), 
      zIndex: Math.min(size + 10, 30) 
    });
    
    // Cache styles for common cluster sizes (don't cache too many to avoid memory issues)
    if (size <= 100) {
      this.clusterStyleCache[cacheKey] = style;
    }
    
    return style;
  }

  // Optimized property style function with caching
  private getPropertyStyle(feature: Feature<Point>): Style {
    const propertyData = feature.get('data') as (Property | PropertyListingDTO);
    if (!propertyData) return new Style(); 

    const propertyType = (this.getPropertyType(propertyData) || 'default').toLowerCase();
    const isPrimary = feature.get('isPrimary') !== false;
    const cacheKey = `${propertyType}-${isPrimary ? 'primary' : 'secondary'}`;
    
    // Return from cache if available
    if (this.styleCache[cacheKey]) {
      return this.styleCache[cacheKey];
    }
    
    // Otherwise create the style
    const fillColor = this.propertyTypeColors[propertyType] || this.propertyTypeColors['default'];
    const priceText = isPrimary ? this.getPrice(propertyData) : '';
    
    const svgSize = isPrimary ? 36 : 28;
    const strokeWidth = isPrimary ? 2 : 1;
    const opacity = isPrimary ? 1 : 0.6;
    
    let svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="${svgSize}" height="${svgSize}" viewBox="0 0 24 24" fill="${fillColor}" opacity="${opacity}"><path d="M12 0C7.802 0 4 3.403 4 7.602C4 11.8 7.469 16.812 12 24C16.531 16.812 20 11.8 20 7.602C20 3.403 16.199 0 12 0Z" stroke="white" stroke-width="${strokeWidth}" />${isPrimary ? `<circle cx="12" cy="8" r="3.5" fill="white" />` : ''}</svg>`;
    
    // Create a new style and cache it
    const style = new Style({
      image: new Icon({ src: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svgIcon), scale: 1, anchor: [0.5, 1] }),
      text: (isPrimary && priceText && priceText !== 'Price not available') ? new Text({ text: priceText, offsetY: -svgSize - 8, font: 'bold 12px Arial', fill: new Fill({ color: '#fff' }), stroke: new Stroke({ color: '#000', width: 3 }), scale: 1.1, backgroundFill: new Fill({color: 'rgba(0,0,0,0.2)'}), padding: [3, 5, 3, 5] }) : undefined,
      zIndex: isPrimary ? 2 : 1
    });
    
    // Store in cache
    this.styleCache[cacheKey] = style;
    return style;
  }

  private getInstituteStyle(feature: Feature<Point>): Style {
    const svgIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24"><path d="M12 0C7.802 0 4 3.403 4 7.602C4 11.8 7.469 16.812 12 24C16.531 16.812 20 11.8 20 7.602C20 3.403 16.199 0 12 0Z" fill="#16a085" stroke="white" stroke-width="1.5" /><path d="M12 3L4 9h2v7h12V9h2L12 3z" fill="white" /></svg>`;
    return new Style({ image: new Icon({ src: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svgIcon), scale: 1, anchor: [0.5, 1]}), zIndex: 3 });
  }

  private displayClusterPopup(primaryProperty: Property | PropertyListingDTO, features: Feature<Point>[], coordinates: number[]): void {
    if (!this.popupContentRef?.nativeElement) return;
    const contentDiv = this.popupContentRef.nativeElement;
    const displayImageUrl = this.getMainImageUrl(primaryProperty);
    const fallbackImageUrl = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';

    const otherProperties = features.slice(1).map(f => f.get('data') as (Property | PropertyListingDTO)).filter(Boolean);

    contentDiv.innerHTML = `
      <div class="popup-header">
        ${displayImageUrl ? `<img src="${displayImageUrl}" alt="${primaryProperty.title}" class="popup-image" onerror="this.src='${fallbackImageUrl}'"/>` : `<div class="popup-image-placeholder">No Image</div>`}
        ${features.length > 1 ? `<div class="cluster-badge">${features.length} properties</div>` : ''}
      </div>
      <h5>${primaryProperty.title}</h5>
      <p class="property-type">${this.getPropertyType(primaryProperty)}</p>
      <p class="address">${this.getPropertyAddress(primaryProperty)}</p>
      <p class="price"><strong>${this.getPrice(primaryProperty)}</strong></p>
      ${otherProperties.length > 0 ? `
        <div class="other-properties">
          <p class="other-heading">Also at this location:</p>
          <ul class="other-list">
            ${otherProperties.slice(0, 3).map(prop => `<li>${this.getPropertyType(prop)}: ${this.getPrice(prop)}</li>`).join('')}
            ${otherProperties.length > 3 ? `<li>+ ${otherProperties.length - 3} more...</li>` : ''}
          </ul></div>` : ''}`;
    
    // Get map size
    const mapSize = this.map?.getSize();
    if (!mapSize) {
      this.showPopup(coordinates);
      return;
    }
    
    // Convert coordinates to pixel position
    const pixel = this.map!.getPixelFromCoordinate(coordinates);
    if (!pixel) {
      this.showPopup(coordinates);
      return;
    }
    
    // For cluster popups, which may be larger, be more aggressive with positioning
    if (pixel[1] > mapSize[1] * 0.4) { // Use a higher threshold to position above
      // Position cluster popups above marker more aggressively
      this.popupOverlay.setPositioning('bottom-center');
      this.popupOverlay.setOffset([0, -30]);
    } else {
      // Position below
      this.popupOverlay.setPositioning('top-center');
      this.popupOverlay.setOffset([0, 15]);
    }
    
    this.popupOverlay.setPosition(coordinates);
    this.isPopupVisible = true;
    
    // Immediately ensure visibility
    this.ensurePopupVisible(coordinates);
  }

  private showSinglePropertyPopup(property: Property | PropertyListingDTO, coordinates: number[]): void {
    if (!this.popupContentRef?.nativeElement) return;

    this.activePopupImages = this.getImageArray(property) || [];
    if (this.activePopupImages.length === 0) {
        const mainImg = this.getMainImageUrl(property);
        if (mainImg) this.activePopupImages = [mainImg];
    }
    this.currentPopupImageIndex = 0;
    this.activePopupPropertyId = this.getNumericId(property);
    this.activePopupPropertySourceType = 'sourceType' in property ? (property.sourceType || null) : null;
    
    const contentDiv = this.popupContentRef.nativeElement;
    const displayImageUrl = this.activePopupImages.length > 0 ? this.activePopupImages[0] : undefined; // Use undefined if no image
    const fallbackImageUrl = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';

    const propLat = this.getLatitude(property);
    const propLng = this.getLongitude(property);
    let multipleProperties = false;
    let nearbyCount = 0;

    if (propLat === undefined || propLng === undefined) {
        console.warn(`Property ${this.getNumericId(property)} has no valid coordinates for popup.`);
        contentDiv.innerHTML = `<h5>${property.title}</h5><p>Location data not available.</p>`;
        this.showPopup(coordinates);
        return; 
    }
    
    const locationKey = `${propLat.toFixed(6)},${propLng.toFixed(6)}`;

    if (this.properties.length > 0) {
      nearbyCount = this.properties.filter(p => {
        const pId = this.getNumericId(p);
        const currentPropId = this.getNumericId(property);
        if (pId === currentPropId) return false;
        const pLat = this.getLatitude(p);
        const pLng = this.getLongitude(p);
        return pLat !== undefined && pLng !== undefined && Math.abs(pLat - propLat) < 0.0001 && Math.abs(pLng - propLng) < 0.0001;
      }).length;
      multipleProperties = nearbyCount > 0;
    }
    
    const bedrooms = this.getBedrooms(property);
    const bathrooms = this.getBathrooms(property);

    contentDiv.innerHTML = `
      <div class="popup-header">
        ${displayImageUrl ? `<img src="${displayImageUrl}" alt="${property.title}" class="popup-image" onerror="this.src='${fallbackImageUrl}'"/>` : `<div class="popup-image-placeholder">No Image</div>`}
        ${multipleProperties ? `<div class="nearby-badge">${nearbyCount + 1} properties at this location</div>` : ''}
      </div>
      <h5>${property.title}</h5>
      <p class="property-type">${this.getPropertyType(property)}</p>
      <p class="address">${this.getPropertyAddress(property)}</p>
      <p class="price"><strong>${this.getPrice(property)}</strong></p>
      ${bedrooms !== undefined ? `<p class="details">Bedrooms: ${bedrooms}</p>` : ''}
      ${bathrooms !== undefined ? `<p class="details">Bathrooms: ${bathrooms}</p>` : ''}
      ${('area' in property && property.area) ? `<p class="details">Area: ${property.area} m²</p>` : ''}
      ${multipleProperties ? `<p class="nearby-note">There are other properties at this location. Zoom in or check property list.</p>` : ''}`;
    this.showPopup(coordinates);
  }

  private showPopup(coordinates: number[]): void {
    if (!this.popupOverlay || !this.map) return;
    
    // Get map size
    const mapSize = this.map.getSize();
    if (!mapSize) return;
    
    // Convert coordinates to pixel position
    const pixel = this.map.getPixelFromCoordinate(coordinates);
    if (!pixel) return;
    
    // More aggressive positioning logic
    // For markers in the bottom 50% of the map, position above
    const isLowerHalf = pixel[1] > mapSize[1] * 0.5;
    
    if (isLowerHalf) {
      // For bottom markers, position popup above marker
      this.popupOverlay.setPositioning('bottom-center');
      this.popupOverlay.setOffset([0, -25]); // More offset to keep away from marker
    } else {
      // For top markers, position below
      this.popupOverlay.setPositioning('top-center');
      this.popupOverlay.setOffset([0, 15]);
    }
    
    // Set position and immediately force auto-panning
    this.popupOverlay.setPosition(coordinates);
    this.isPopupVisible = true;
    
    // Immediately ensure visibility without waiting
    this.ensurePopupVisible(coordinates);
  }
  
  /**
   * Ensures the popup is fully visible on the map by panning if needed
   */
  private ensurePopupVisible(coordinates: number[]): void {
    if (!this.map || !this.isPopupVisible || !this.popupContainerRef?.nativeElement) return;
    
    // Short delay to let the DOM update
    setTimeout(() => {
      const popupElement = this.popupContainerRef.nativeElement;
      const mapSize = this.map!.getSize();
      if (!mapSize) return;
      
      const view = this.map!.getView();
      const pixel = this.map!.getPixelFromCoordinate(coordinates);
      if (!pixel) return;
      
      const popupRect = popupElement.getBoundingClientRect();
      const mapRect = this.map!.getTargetElement().getBoundingClientRect();
      
      // Calculate popup boundaries
      const popupLeft = popupRect.left - mapRect.left;
      const popupRight = popupRect.right - mapRect.left;
      const popupTop = popupRect.top - mapRect.top;
      const popupBottom = popupRect.bottom - mapRect.top;
      
      // Calculate how much we need to pan (with larger margins)
      let panX = 0;
      let panY = 0;
      const margin = 30; // Increased margin
      
      // Check horizontal overflow
      if (popupLeft < margin) {
        panX = popupLeft - margin;
      } else if (popupRight > mapSize[0] - margin) {
        panX = popupRight - mapSize[0] + margin;
      }
      
      // Check vertical overflow - more aggressive for bottom overflow
      if (popupTop < margin) {
        panY = popupTop - margin;
      } else if (popupBottom > mapSize[1] - margin) {
        // More aggressive correction for bottom overflow
        panY = popupBottom - mapSize[1] + margin + 10;
      }
      
      // Apply panning if needed - faster animation
      if (panX !== 0 || panY !== 0) {
        const currentCenter = view.getCenter();
        if (currentCenter) {
          const resolution = view.getResolution() || 1;
          const newCenter = [
            currentCenter[0] + panX * resolution,
            currentCenter[1] + panY * resolution
          ];
          view.animate({
            center: newCenter,
            duration: 150 // Faster animation
          });
        }
      }
    }, 10); // Minimal delay
  }

  closePopup(event?: Event): void {
    if (event) { event.preventDefault(); event.stopPropagation(); }
    this.isPopupVisible = false;
    if (this.popupOverlay) this.popupOverlay.setPosition(undefined);
  }

  private optimizedUpdateFeatures(features: Feature<Point>[]): void {
    if (!this.vectorSource) return;
    
    this.vectorSource.clear();
    this.vectorSource.addFeatures(features);
    
    if (this.vectorLayer) {
      this.vectorLayer.changed();
    }
  }

  private updateMapFeatures(): void {
    if (!this.map) return;
    this.vectorSource.clear();
    this.instituteVectorSource.clear();

    let validFeatures = 0, invalidFeatures = 0;
    const locationMap: {[key: string]: number[]} = {}; // Stores property IDs
    const duplicateLocations: string[] = []; // Stores locationKeys

    this.properties.forEach(prop => {
      const lat = this.getLatitude(prop);
      const lng = this.getLongitude(prop);
      if (lat !== undefined && lng !== undefined) {
        const locationKey = `${lat.toFixed(5)},${lng.toFixed(5)}`;
        const propId = this.getNumericId(prop);
        if (locationKey in locationMap) {
          if (!duplicateLocations.includes(locationKey)) duplicateLocations.push(locationKey);
          locationMap[locationKey].push(propId);
        } else {
          locationMap[locationKey] = [propId];
        }
      }
    });

    const features: Feature<Point>[] = [];
    const pseudoRandom = (n: number) => Math.abs(Math.sin(n * 12345.6789) * 10000) % 1;

    this.properties.forEach(prop => {
      let finalLat = this.getLatitude(prop);
      let finalLon = this.getLongitude(prop);

      if (finalLat !== undefined && finalLon !== undefined) {
        const locationKey = `${finalLat.toFixed(5)},${finalLon.toFixed(5)}`;
        const currentPropId = this.getNumericId(prop);

        if (duplicateLocations.includes(locationKey)) {
          const propIdsAtLocation = locationMap[locationKey] || [];
          const propIndex = propIdsAtLocation.indexOf(currentPropId);
          
          if (propIndex > 0) { // Only jitter if not the first property at location
            const seed = currentPropId + propIndex; // Use propId for consistent jitter
            const rand1 = pseudoRandom(seed);
            const rand2 = pseudoRandom(seed + 100);
            const shouldJitter = rand1 > 0.2 || propIdsAtLocation.length > 10;

            if (shouldJitter) { 
              const baseJitterAmount = 0.0005; 
              const spreadFactor = Math.min(1.5, 0.7 + (propIdsAtLocation.length / 100));
              const angle = (propIndex * (2.4 * Math.PI / propIdsAtLocation.length)) + (rand1 * Math.PI / 4);
              const distance = baseJitterAmount * spreadFactor * (0.3 + (0.7 * propIndex / propIdsAtLocation.length));
              finalLat += distance * Math.sin(angle) * (0.8 + rand2 * 0.4);
              finalLon += distance * Math.cos(angle) * (0.8 + rand1 * 0.4);
            }
          }
        }
        
        const coords = fromLonLat([finalLon, finalLat]);
        if (!isNaN(coords[0]) && !isNaN(coords[1])) {
          const isPrimary = this.visiblePropertyIds.includes(currentPropId);
          const feature = new Feature({ geometry: new Point(coords), data: prop, isPrimary: isPrimary });
          features.push(feature);
          validFeatures++;
        } else invalidFeatures++;
      } else invalidFeatures++;
    });
    
    if (features.length > 0) {
      this.optimizedUpdateFeatures(features);
    }

    if (this.selectedInstitute) {
        const instLat = this.selectedInstitute.latitude;
        const instLng = this.selectedInstitute.longitude;
        if (instLat != null && instLng != null) {
            try {
                const instituteCoords = fromLonLat([instLng, instLat]);
                if (!isNaN(instituteCoords[0]) && !isNaN(instituteCoords[1])) {
                    const instituteFeature = new Feature({ geometry: new Point(instituteCoords), data: this.selectedInstitute });
                    this.instituteVectorSource.addFeature(instituteFeature);
                    const view = this.map.getView();
                    if (features.length === 0) {
                        view.animate({ center: instituteCoords, zoom: 13, duration: 1000 });
                    } else {
                        const allFeaturesSource = new VectorSource({ features: [...features, instituteFeature] });
                        view.fit(allFeaturesSource.getExtent(), { padding: [70, 70, 70, 70], duration: 1000, maxZoom: 15 });
                    }
                }
            } catch(e) { console.error("Error processing institute coordinates: ", e); }
        }
    } else if (features.length > 0 && this.vectorSource.getFeatures().length > 0) {
      try {
        this.map.getView().fit(this.vectorSource.getExtent(), { padding: [50, 50, 50, 50], duration: 1000, maxZoom: 14 });
      } catch (e) { console.error("Error fitting map to extent: ", e); }
    }
    this.updateClusterDistance();
    this.closePopup();
  }

  private handlePropertyClick(propertyId: number, coordinates: number[]): void {
    // First emit the property selected event
    this.propertySelected.emit(propertyId);
    
    // Then animate to the location (but don't zoom if already zoomed)
    const view = this.map?.getView();
    const currentZoom = view?.getZoom() || 0;
    
    // Only zoom if not already at a good level
    const shouldZoom = currentZoom < 14;
    this.animateToLocation(coordinates, shouldZoom);
    
    // Wait for animation to complete before showing popup
    setTimeout(() => {
      const property = this.properties.find(p => this.getNumericId(p) === propertyId);
      if (property) {
        this.showSinglePropertyPopup(property, coordinates);
      }
    }, 300);
  }
  
  private animateToLocation(coordinates: number[], zoomCloser: boolean = false): void {
    if (!this.map) return;
    const view = this.map.getView();
    const currentZoom = view.getZoom() || 0;
    const targetZoom = zoomCloser ? Math.min(16, Math.max(currentZoom, 14)) : currentZoom;
    view.animate({ center: coordinates, zoom: targetZoom, duration: 800, easing: (t) => t < 0.5 ? 2*t*t : -1+(4-2*t)*t });
  }

  private updateVisibleProperties(): void {
    if (!this.vectorSource || !this.map) return;
    const features = this.vectorSource.getFeatures();
    let updatedCount = 0;
    features.forEach(feature => {
      const propertyData = feature.get('data') as (Property | PropertyListingDTO);
      if (!propertyData) return; // Should have data
      const propId = this.getNumericId(propertyData);
      const isVisible = this.visiblePropertyIds.includes(propId);
      if (feature.get('isPrimary') !== isVisible) {
        feature.set('isPrimary', isVisible);
        updatedCount++;
      }
    });
    if (updatedCount > 0 && this.vectorLayer) this.vectorLayer.changed();
  }

  private highlightSelectedProperty(): void {
    if (!this.map || this.selectedPropertyId === null) return; // Check for null
    const selectedProperty = this.properties.find(p => this.getNumericId(p) === this.selectedPropertyId);
    if (!selectedProperty) return;
    
    const lat = this.getLatitude(selectedProperty);
    const lng = this.getLongitude(selectedProperty);
    if (lng === undefined || lat === undefined) return;
    
    const coordinates = fromLonLat([lng, lat]);
    this.animateToLocation(coordinates, true);
    setTimeout(() => this.showSinglePropertyPopup(selectedProperty, coordinates), 300);
    
    const features = this.vectorSource.getFeatures();
    const selectedFeature = features.find(f => {
      const featureData = f.get('data') as (Property | PropertyListingDTO);
      return featureData && (this.getNumericId(featureData) === this.selectedPropertyId);
    });
    if (selectedFeature) this.flashFeature(selectedFeature);
  }
  
  private flashFeature(feature: Feature<Point>): void {
    const originalStyle = feature.getStyle() as Style;
    if (!originalStyle) return; // Cannot flash if no original style

    const flashImage = new CircleStyle({ radius: 15, fill: new Fill({ color: 'rgba(0, 123, 255, 0.8)' }), stroke: new Stroke({ color: '#ffffff', width: 3 }) });
    const flashStyle = new Style({ image: flashImage, text: originalStyle.getText() || undefined, zIndex: 10 });
    feature.setStyle(flashStyle);
    
    let phase = 0;
    const animationId = setInterval(() => {
      phase += 0.1;
      const scale = 1 + 0.3 * Math.sin(phase * Math.PI);
      const animatedImage = new CircleStyle({ radius: 15 * scale, fill: new Fill({ color: `rgba(0, 123, 255, ${0.8 - 0.3 * Math.sin(phase * Math.PI)})`}), stroke: new Stroke({ color: '#ffffff', width: 3 }) });
      feature.setStyle(new Style({image: animatedImage, text: originalStyle.getText() || undefined, zIndex: 10}));
      if (phase >= 2) { clearInterval(animationId); setTimeout(() => feature.setStyle(originalStyle), 100); }
    }, 50);
  }

  prevPopupImage(event: Event): void {
    event.preventDefault();
    if (this.activePopupImages.length <= 1) return;
    this.currentPopupImageIndex = (this.currentPopupImageIndex - 1 + this.activePopupImages.length) % this.activePopupImages.length;
    this.updatePopupImage();
  }
  
  nextPopupImage(event: Event): void {
    event.preventDefault();
    if (this.activePopupImages.length <= 1) return;
    this.currentPopupImageIndex = (this.currentPopupImageIndex + 1) % this.activePopupImages.length;
    this.updatePopupImage();
  }
  
  private updatePopupImage(): void {
    if (this.activePopupImages.length === 0 || !this.popupContainerRef?.nativeElement) return;
    const currentImage = this.activePopupImages[this.currentPopupImageIndex];
    const imgElement = this.popupContainerRef.nativeElement.querySelector('.popup-image') as HTMLImageElement;
    if (imgElement) imgElement.src = currentImage || ''; 
  }

  // Add method to clear caches when necessary (e.g., when component is destroyed)
  private clearStyleCaches(): void {
    this.styleCache = {};
    this.clusterStyleCache = {};
  }

  // Update ngOnDestroy to clear caches
  ngOnDestroy(): void {
    this.clearStyleCaches();
    if (this.map) { this.map.setTarget(undefined); this.map = null; }
    if (this.popupOverlay) this.popupOverlay.dispose();
    if (this.resizeObserver) { this.resizeObserver.disconnect(); this.resizeObserver = null; }
  }
}
