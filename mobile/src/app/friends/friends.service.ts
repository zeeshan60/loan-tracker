import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AddFriend } from './friends.store';
import { Friend } from './model';
import { PRIVATE_API } from '../constants';

@Injectable({
  providedIn: 'root'
})
export class FriendsService {
  readonly http = inject(HttpClient);

  constructor() { }

  loadAllFriends(): Observable<{ data: { friends: Friend[]}}> {
    return this.http.get<{ data: { friends: Friend[]}}>(PRIVATE_API + '/friends');
  }

  addFriend(friend: AddFriend): Observable<any> {
    return this.http.post(PRIVATE_API + '/friends/add', friend)
  }
}
