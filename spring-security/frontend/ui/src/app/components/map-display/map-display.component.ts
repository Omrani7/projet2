import { Component, Input, OnChanges, SimpleChanges, ElementRef, AfterViewInit, OnDestroy, ViewChild, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PropertyListingDTO } from '../../../../../src/app/models/property-listing.dto';
import { Institute } from '../../../../../src/app/models/institute.model';

// OpenLayers imports
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';
import { fromLonLat, transform } from 'ol/proj'; // Import transform for coordinate conversion if needed
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Style from 'ol/style/Style';
import Icon from 'ol/style/Icon';
import Fill from 'ol/style/Fill';
import Stroke from 'ol/style/Stroke';
import CircleStyle from 'ol/style/Circle';
import {FeatureLike} from "ol/Feature";
import Overlay from 'ol/Overlay';
import { getCenter } from 'ol/extent';

@Component({
  selector: 'app-map-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './map-display.component.html',
  styleUrls: ['./map-display.component.css']
})
export class MapDisplayComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() properties: PropertyListingDTO[] = [];
  @Input() selectedInstitute: Institute | null = null;

  @ViewChild('popupContainer') popupContainerRef!: ElementRef<HTMLDivElement>;
  @ViewChild('popupContent') popupContentRef!: ElementRef<HTMLDivElement>;

  private map: Map | null = null; // Typed as ol/Map
  mapId = 'ol-map-' + Math.random().toString(36).substring(2); // mapId is public to be accessible by template
  private vectorSource: VectorSource;
  private instituteVectorSource: VectorSource;
  private popupOverlay!: Overlay;
  isPopupVisible = false;

  // Default map center (Tunis)
  private defaultCenter = fromLonLat([10.1815, 36.8065]);
  private defaultZoom = 7;

  constructor(private elementRef: ElementRef, private renderer: Renderer2) {
    this.vectorSource = new VectorSource();
    this.instituteVectorSource = new VectorSource();
  }

  ngAfterViewInit(): void {
    if (!this.map) { // Ensure map is initialized only once
        this.initMap();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.map) { // Ensure map is initialized before updating features
      if (changes['properties'] || changes['selectedInstitute']) {
         this.updateMapFeatures();
      }
    }
  }

  private initMap(): void {
    const propertyVectorLayer = new VectorLayer({
        source: this.vectorSource,
        style: this.getPropertyStyle.bind(this) // Style function for property features
    });
    const instituteVectorLayer = new VectorLayer({
        source: this.instituteVectorSource,
        style: this.getInstituteStyle.bind(this) // Style function for institute features
    });

    this.popupOverlay = new Overlay({
      element: this.popupContainerRef.nativeElement,
      autoPan: { animation: { duration: 250 } }
    });

    this.map = new Map({
      target: this.mapId,
      layers: [
        new TileLayer({ source: new OSM() }),
        propertyVectorLayer,
        instituteVectorLayer
      ],
      overlays: [this.popupOverlay],
      view: new View({
        center: this.defaultCenter,
        zoom: this.defaultZoom,
        constrainResolution: true // Prevents zooming to intermediate levels
      })
    });

    this.map.on('click', (event) => {
      const clickedFeature = this.map!.forEachFeatureAtPixel(event.pixel, (featureLike, layer) => {
          // Check if the feature belongs to the property layer
          if (layer === propertyVectorLayer) {
              return featureLike as Feature<Point>; // Cast to Feature with Point geometry
          }
          return undefined;
      });

      if (clickedFeature) {
        const propertyData = clickedFeature.get('data') as PropertyListingDTO;
        this.displayPropertyPopup(propertyData, clickedFeature.getGeometry()!.getCoordinates());
      } else {
        this.closePopup();
      }
    });
    this.updateMapFeatures(); // Initial population of features
  }

  private getPropertyStyle(feature: FeatureLike, resolution: number): Style {
    // Basic style for properties - can be enhanced (e.g. show price)
    return new Style({
        image: new CircleStyle({
            radius: 7,
            fill: new Fill({ color: 'rgba(255, 0, 0, 0.7)' }), // Red circle
            stroke: new Stroke({ color: '#ffffff', width: 2 })
        })
    });
  }

  private getInstituteStyle(feature: FeatureLike, resolution: number): Style {
    // Basic style for selected institute - can be a different icon or color
    return new Style({
        image: new CircleStyle({
            radius: 9,
            fill: new Fill({ color: 'rgba(0, 0, 255, 0.8)' }), // Blue circle for institute
            stroke: new Stroke({ color: '#ffffff', width: 2 })
        })
        // Or use an Icon style:
        // image: new Icon({
        //   anchor: [0.5, 46], // Example anchor point
        //   anchorXUnits: 'fraction',
        //   anchorYUnits: 'pixels',
        //   src: 'assets/icons/university-pin.png' // Path to your institute icon
        // })
    });
  }

  // Helper method to clean image URLs (remove leading @ symbol)
  private cleanImageUrl(url: string | undefined | null): string | null {
    console.log('Map component - Original image URL:', url);
    if (!url) {
      console.log('Map component - No URL provided, returning null');
      return null;
    }

    // Handle @https:// format
    if (url.startsWith('@')) {
      const cleanedUrl = url.substring(1);
      console.log('Map component - URL starts with @, cleaned URL:', cleanedUrl);
      return cleanedUrl;
    }

    // Handle URLs that are already normal
    console.log('Map component - URL is normal, using as is');
    return url;
  }

  private displayPropertyPopup(property: PropertyListingDTO, coordinates: number[]): void {
    const contentDiv = this.popupContentRef.nativeElement;
    // Use our helper method to clean the image URL
    const cleanedImageUrl = this.cleanImageUrl(property.mainImageUrl);

    const fallbackImageUrl = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';

    // Simple content for now, matching screenshot elements can be complex
    contentDiv.innerHTML = `
      <div class="popup-header">
        ${cleanedImageUrl ?
          `<img src="${cleanedImageUrl}" alt="${property.title}" class="popup-image" onerror="this.src='${fallbackImageUrl}'"/>` :
          `<div class="popup-image-placeholder">No Image</div>`}
      </div>
      <h5>${property.title}</h5>
      <p class="address">${property.location || 'Address not available'}</p>
      <p class="price">From <strong>${property.price ? (property.price + ' TND/month') : 'Price not available'}</strong></p>
    `;
    this.popupOverlay.setPosition(coordinates);
    this.isPopupVisible = true;
  }

  closePopup(event?: Event): void {
    if (event) event.preventDefault();
    this.isPopupVisible = false;
    this.popupOverlay.setPosition(undefined); // Setting position to undefined effectively hides it
  }

  private updateMapFeatures(): void {
    if (!this.map) return;

    this.vectorSource.clear();
    this.instituteVectorSource.clear();

    const features: Feature[] = [];
    this.properties.forEach(prop => {
      if (prop.latitude != null && prop.longitude != null) {
        const feature = new Feature({
          geometry: new Point(fromLonLat([prop.longitude, prop.latitude])),
          data: prop // Attach full property data to feature
        });
        features.push(feature);
      }
    });
    if (features.length > 0) {
        this.vectorSource.addFeatures(features);
    }

    if (this.selectedInstitute && this.selectedInstitute.latitude != null && this.selectedInstitute.longitude != null) {
      const instituteFeature = new Feature({
        geometry: new Point(fromLonLat([this.selectedInstitute.longitude, this.selectedInstitute.latitude])),
        data: this.selectedInstitute
      });
      this.instituteVectorSource.addFeature(instituteFeature);

      // Auto-pan/zoom logic (initial commented out)
      const view = this.map.getView();
      if (features.length === 0) { // Only institute selected, no properties
        view.animate({ center: instituteFeature.getGeometry()!.getCoordinates(), zoom: 13, duration: 1000 });
      } else {
        // Fit to all features if properties also exist, or just institute if no properties
        const extentSource = new VectorSource({ features: [...features, instituteFeature] });
        view.fit(extentSource.getExtent(), { padding: [70, 70, 70, 70], duration: 1000, maxZoom: 16 });
      }
    } else if (features.length > 0) {
        // If no institute selected but there are properties, fit view to properties
        // this.map.getView().fit(this.vectorSource.getExtent(), { padding: [50, 50, 50, 50], duration: 1000, maxZoom: 15 });
    } else {
        // No institute, no properties, reset to default view
        // this.map.getView().animate({ center: this.defaultCenter, zoom: this.defaultZoom, duration: 1000 });
    }
    this.closePopup(); // Close any open popup when features are updated
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.setTarget(undefined);
      this.map = null;
    }
    if (this.popupOverlay) {
        this.popupOverlay.dispose();
    }
  }
}
