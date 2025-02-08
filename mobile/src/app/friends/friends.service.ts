import { inject, Injectable } from '@angular/core';
import { map, Observable, of, tap, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FriendsService {
  readonly http = inject(HttpClient);

  constructor() { }

  loadAllFriends(): Observable<any> {
    return timer(0)
      .pipe(
        map((value) => [])
      );
  }

  createFriend(friend: { name: string }): Observable<any> {
    return timer(100)
      .pipe(
        map((value) => [])
      );
  }
}
