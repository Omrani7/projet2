import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PropertyListingDTO } from '../../../../../src/app/models/property-listing.dto';
import { Page } from '../../../../../src/app/models/page.model';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-property-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './property-list.component.html',
  styleUrls: ['./property-list.component.css']
})
export class PropertyListComponent implements OnChanges, OnInit {
  @Input() propertiesPage: Page<PropertyListingDTO> | null = null;
  @Input() isLoading: boolean = false;
  @Input() errorMessage: string | null = null;

  @Output() pageChanged = new EventEmitter<number>();
  // @Output() propertyHovered = new EventEmitter<PropertyListingDTO | null>(); // For later map interaction
  // @Output() propertyClicked = new EventEmitter<PropertyListingDTO>(); // For later map interaction or detail view

  // Make console available to the template
  console = console;

  // Cache of converted images
  private imageCache = new Map<string, SafeUrl>();

  constructor(
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
    const initTimestamp = Date.now();
    console.log(`[${initTimestamp}] PropertyListComponent ngOnInit CALLED.`);
    if (this.propertiesPage && this.propertiesPage.content) {
      console.log(`[${initTimestamp}] ngOnInit: propertiesPage.content HAS ${this.propertiesPage.content.length} items. Pre-loading images.`);
      this.propertiesPage.content.forEach(prop => {
        if (prop.mainImageUrl) {
          // console.log(`[${initTimestamp}] ngOnInit: Calling getImageAsDataUrl for:`, prop.mainImageUrl); // Optional: very verbose
          this.getImageAsDataUrl(prop.mainImageUrl);
        }
      });
    } else {
      console.log(`[${initTimestamp}] ngOnInit: propertiesPage.content is NULL or EMPTY. No images to pre-load initially.`);
      if (this.propertiesPage) {
        console.log(`[${initTimestamp}] ngOnInit: this.propertiesPage exists, but content is:`, this.propertiesPage.content);
      } else {
        console.log(`[${initTimestamp}] ngOnInit: this.propertiesPage is NULL.`);
      }
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    const changesTimestamp = Date.now();
    console.log(`[${changesTimestamp}] PropertyListComponent ngOnChanges CALLED. Changes:`, JSON.stringify(changes));

    if (changes['propertiesPage']) {
      const ppChange = changes['propertiesPage'];
      console.log(`[${changesTimestamp}] ngOnChanges: propertiesPage CHANGED.`);
      console.log(`[${changesTimestamp}] ngOnChanges: Previous propertiesPage:`, ppChange.previousValue);
      console.log(`[${changesTimestamp}] ngOnChanges: Current propertiesPage:`, ppChange.currentValue);

      if (ppChange.currentValue && ppChange.currentValue.content) {
        console.log(`[${changesTimestamp}] ngOnChanges: Current propertiesPage.content HAS ${ppChange.currentValue.content.length} items. Pre-loading images.`);
        ppChange.currentValue.content.forEach((prop: PropertyListingDTO) => {
          if (prop.mainImageUrl) {
            // console.log(`[${changesTimestamp}] ngOnChanges: Calling getImageAsDataUrl for:`, prop.mainImageUrl); // Optional: very verbose
            this.getImageAsDataUrl(prop.mainImageUrl);
          }
        });
      } else {
        console.log(`[${changesTimestamp}] ngOnChanges: Current propertiesPage.content is NULL or EMPTY.`);
        if (ppChange.currentValue) {
          console.log(`[${changesTimestamp}] ngOnChanges: ppChange.currentValue exists, but content is:`, ppChange.currentValue.content);
        } else {
          console.log(`[${changesTimestamp}] ngOnChanges: ppChange.currentValue is NULL.`);
        }
      }
    }
    if (changes['isLoading']) {
       console.log(`[${changesTimestamp}] ngOnChanges: isLoading changed to:`, changes['isLoading'].currentValue);
    }
    if (changes['errorMessage']) {
       console.log(`[${changesTimestamp}] ngOnChanges: errorMessage changed to:`, changes['errorMessage'].currentValue);
    }
  }

  // Helper method to clean image URLs (remove leading @ symbol)
  cleanImageUrl(url: string | undefined | null): string {
    console.log('Original image URL:', url);
    if (!url) {
      console.log('No URL provided, using fallback');
      return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='; // Base64 SVG "No Image"
    }

    // Handle @https:// format
    if (url.startsWith('@')) {
      const cleanedUrl = url.substring(1);
      console.log('URL starts with @, cleaned URL:', cleanedUrl);
      return cleanedUrl;
    }

    // Handle URLs that are already normal
    console.log('URL is normal, using as is');
    return url;
  }

  // Get image as data URL to avoid CORS issues
  getImageAsDataUrl(url: string | undefined | null): SafeUrl {
    const functionStartTime = Date.now();
    console.log(`[${functionStartTime}] getImageAsDataUrl CALLED for:`, url);

    if (!url) {
      console.log(`[${functionStartTime}] getImageAsDataUrl: No URL provided, returning NO_IMAGE_FALLBACK for:`, url);
      return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';
    }

    // Clean the URL if needed
    const cleanedUrl = this.cleanImageUrl(url);
    console.log(`[${functionStartTime}] getImageAsDataUrl: Cleaned URL:`, cleanedUrl);

    // Check if we've already converted this image
    // if (this.imageCache.has(cleanedUrl)) {
    //   const cachedImage = this.imageCache.get(cleanedUrl)!;
    //   console.log(`[${functionStartTime}] getImageAsDataUrl: Returning CACHED image for:`, cleanedUrl, cachedImage);
    //   return cachedImage;
    // }

    // Default fallback
    const fallback = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5Mb2FkaW5nLi4uPC90ZXh0Pjwvc3ZnPg==';
    console.log(`[${functionStartTime}] getImageAsDataUrl: Setting LOADING_FALLBACK for:`, cleanedUrl);
    this.imageCache.set(cleanedUrl, fallback); // Temporarily set loading, will be replaced

    // Always use proxy for tayara.tn due to CORS restrictions
    if (cleanedUrl.includes('tayara.tn')) {
      console.log(`[${functionStartTime}] getImageAsDataUrl: URL contains 'tayara.tn'. USING PROXY for:`, cleanedUrl);
      this.tryProxyFetch(cleanedUrl, functionStartTime);
    } else {
      // For other domains, try direct fetch first (this path might need similar proxy logic if CORS issues arise)
      console.log(`[${functionStartTime}] getImageAsDataUrl: URL does NOT contain 'tayara.tn'. Trying DIRECT FETCH for:`, cleanedUrl);
      fetch(cleanedUrl)
        .then(response => {
          console.log(`[${functionStartTime}] getImageAsDataUrl: Direct fetch response for ${cleanedUrl}:`, response);
          if (!response.ok) {
            throw new Error(`Direct fetch failed with status ${response.status}`);
          }
          return response.blob();
        })
        .then(blob => {
          console.log(`[${functionStartTime}] getImageAsDataUrl: Direct fetch blob for ${cleanedUrl}:`, blob);
          const reader = new FileReader();
          reader.onload = () => {
            const rawDataUrl = reader.result as string;
            console.log(`[${functionStartTime}] getImageAsDataUrl: Direct fetch raw data URL for ${cleanedUrl} (length ${rawDataUrl.length}):`, rawDataUrl.substring(0,100) + '...');
            const safeDataUrl = this.sanitizer.bypassSecurityTrustUrl(rawDataUrl);
            console.log(`[${functionStartTime}] getImageAsDataUrl: Direct fetch SANITIZED data URL for ${cleanedUrl}:`, safeDataUrl);
            this.imageCache.set(cleanedUrl, safeDataUrl);
            console.log(`[${functionStartTime}] getImageAsDataUrl: WROTE direct fetch result to imageCache for ${cleanedUrl}`);
          };
          reader.readAsDataURL(blob);
        })
        .catch(err => {
          console.error(`[${functionStartTime}] getImageAsDataUrl: Direct fetch FAILED for ${cleanedUrl}, trying proxy. Error:`, err);
          this.tryProxyFetch(cleanedUrl, functionStartTime); // Fallback to proxy
        });
    }

    console.log(`[${functionStartTime}] getImageAsDataUrl: Returning initial LOADING_FALLBACK for ${cleanedUrl} while async fetch proceeds.`);
    return fallback;
  }

  private tryProxyFetch(url: string, callTime: number): void {
    const proxyUrl = `/api/v1/image-proxy?url=${encodeURIComponent(url)}`;
    console.log(`[${callTime}] tryProxyFetch: Fetching via proxy: ${proxyUrl} for original url: ${url}`);

    this.http.get(proxyUrl, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        console.log(`[${callTime}] tryProxyFetch: Proxy fetch SUCCESSFUL for ${url}, blob size: ${blob.size}, type: ${blob.type}`);
        const reader = new FileReader();
        reader.onload = () => {
          const rawDataUrl = reader.result as string;
          console.log(`[${callTime}] tryProxyFetch: Raw data URL from proxy for ${url} (length ${rawDataUrl.length}):`, rawDataUrl.substring(0, 100) + '...');
          const safeDataUrl = this.sanitizer.bypassSecurityTrustUrl(rawDataUrl);
          console.log(`[${callTime}] tryProxyFetch: SANITIZED data URL from proxy for ${url}:`, safeDataUrl);
          this.imageCache.set(url, safeDataUrl);
          console.log(`[${callTime}] tryProxyFetch: WROTE proxy result to imageCache for ${url}`);
        };
        reader.readAsDataURL(blob);
      },
      error: (err) => {
        console.error(`[${callTime}] tryProxyFetch: FAILED to load image through proxy for ${url}. Error:`, err);
        const noImageFallback = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';
        this.imageCache.set(url, noImageFallback);
        console.log(`[${callTime}] tryProxyFetch: SET NO_IMAGE_FALLBACK in imageCache for ${url} after proxy error.`);
      }
    });
  }

  handleImageError(event: Event, property: PropertyListingDTO): void {
    const target = event.target as HTMLImageElement;
    console.error(
      `IMAGE TAG FAILED TO LOAD SRC FOR: '${property.title}'`,
      {
        cleanedUrl: this.cleanImageUrl(property.mainImageUrl),
        problematicSrc: target ? target.src : 'UNKNOWN (target not HTMLImageElement)',
        originalEvent: event
      }
    );
    // Optionally, you could try to set a fallback src directly here if needed
    // if (target) target.src = this.NO_IMAGE_FALLBACK;
  }

  onPageChange(pageNumber: number): void {
    if (this.propertiesPage && pageNumber >= 0 && pageNumber < this.propertiesPage.totalPages) {
      this.pageChanged.emit(pageNumber);
    }
  }

  // Helper for pagination: an array of page numbers to display
  get pageNumbers(): number[] {
    if (!this.propertiesPage || this.propertiesPage.totalPages <= 1) {
      return [];
    }
    // Simple range for now, can be made more sophisticated (e.g., with ellipses)
    const maxPagesToShow = 5;
    const currentPage = this.propertiesPage.number;
    const totalPages = this.propertiesPage.totalPages;
    let startPage: number, endPage: number;

    if (totalPages <= maxPagesToShow) {
      startPage = 0;
      endPage = totalPages - 1;
    } else {
      if (currentPage <= Math.floor(maxPagesToShow / 2)) {
        startPage = 0;
        endPage = maxPagesToShow - 1;
      } else if (currentPage + Math.floor(maxPagesToShow / 2) >= totalPages) {
        startPage = totalPages - maxPagesToShow;
        endPage = totalPages - 1;
      } else {
        startPage = currentPage - Math.floor(maxPagesToShow / 2);
        endPage = currentPage + Math.floor(maxPagesToShow / 2);
      }
    }
    return Array.from(Array((endPage - startPage) + 1).keys()).map(i => startPage + i);
  }
}
