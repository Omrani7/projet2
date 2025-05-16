import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs'; // Import 'of' for returning mock observables
import { Institute} from "../../../../src/app/models/institute.model";
import { environment} from "../../../../src/environments/environment";

@Injectable({
  providedIn: 'root'
})
export class InstituteService {

  private apiUrl = `${environment.apiBaseUrl}/institutes`; // Corrected: Removed redundant /api/v1

  constructor(private http: HttpClient) { }

  /**
   * Searches for institutes by a name fragment.
   * @param nameFragment The fragment of the institute name to search for.
   * @returns An Observable array of matching Institutes.
   */
  searchInstitutes(nameFragment: string): Observable<Institute[]> {
    if (!nameFragment || nameFragment.trim() === '') {
      return of([]); // Return empty array if search term is empty
    }
    const params = new HttpParams().set('name', nameFragment);
    return this.http.get<Institute[]>(`${this.apiUrl}/search`, { params }); // Corrected endpoint and uncommented HTTP call
  }
}
