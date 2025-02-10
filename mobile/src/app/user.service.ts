import { Injectable } from '@angular/core';
import { getAuth } from '@angular/fire/auth';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor() { }

  getUser() {
    return getAuth().currentUser;
  }
}
