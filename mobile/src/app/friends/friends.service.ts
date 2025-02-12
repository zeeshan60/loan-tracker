import { inject, Injectable } from '@angular/core';
import { map, Observable, of, tap, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AddFriend } from './friends.store';
import { Friend } from './model';

@Injectable({
  providedIn: 'root'
})
export class FriendsService {
  readonly http = inject(HttpClient);

  constructor() { }

  loadAllFriends(): Observable<{ data: { friends: Friend[]}}> {
    return this.http.get<{ data: { friends: Friend[]}}>(environment.apiBaseUrl + '/friends');
  }

  addFriend(friend: AddFriend): Observable<any> {
    return this.http.post(environment.apiBaseUrl + '/friends/add', friend)
  }
}
