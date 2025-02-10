import { inject, Injectable } from '@angular/core';
import { map, Observable, of, tap, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FriendsService {
  readonly http = inject(HttpClient);

  constructor() { }

  loadAllFriends(): Observable<any> {
    return this.http.get(environment.apiBaseUrl + '/friends');
    // return timer(1000)
    //   .pipe(
    //     map((value) => [])
    //   );
  }

  createFriend(friend: { name: string, email: string|null, phone: string }): Observable<any> {
    throw new Error('not implemented.');
    return timer(100)
      .pipe(
        map((value) => [])
      );
  }
}
