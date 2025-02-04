import { inject, Injectable } from '@angular/core';
import { map, Observable, of, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FriendsService {
  readonly http = inject(HttpClient);

  constructor() { }

  loadAllFriends(): Observable<any> {
    return timer(2000)
      .pipe(
        map((value) => [])
      );
  }
}
