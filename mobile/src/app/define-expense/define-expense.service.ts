import { inject, Injectable } from '@angular/core';
import { DefineExpenseComponent } from './define-expense.component';
import { FriendsStore } from '../friends/friends.store';
import { Router } from '@angular/router';
import { ModalService } from '../modal.service';

@Injectable({
  providedIn: 'root'
})
export class DefineExpenseService {
  readonly friendsStore = inject(FriendsStore);
  readonly router = inject(Router);
  readonly modalService = inject(ModalService);

  constructor() { }

  async openDefineExpenseModal() {
    let selectedFriend = null;
    if (this.router.url.endsWith('tabs/friends')) {
      selectedFriend = this.friendsStore.selectedFriend();
    }
    await this.modalService.showModal({
      component: DefineExpenseComponent,
      componentProps: { friend: selectedFriend }
    })
  }
}
