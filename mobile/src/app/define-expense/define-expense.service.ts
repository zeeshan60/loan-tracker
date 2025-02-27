import { inject, Injectable } from '@angular/core';
import { DefineExpenseComponent } from './define-expense.component';
import { FriendsStore } from '../friends/friends.store';
import { ModalController } from '@ionic/angular/standalone';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class DefineExpenseService {
  readonly friendsStore = inject(FriendsStore);
  readonly modalCtrl = inject(ModalController);
  readonly router = inject(Router);
  public defineExpenseModalInstance: HTMLIonModalElement|null = null;

  constructor() { }

  async openDefineExpenseModal() {
    let selectedFriend = null;
    if (this.router.url.endsWith('tabs/friends')) {
      selectedFriend = this.friendsStore.selectedFriend();
    }
    this.defineExpenseModalInstance = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: { friend: selectedFriend }
    })
    this.defineExpenseModalInstance.present();
  }
}
