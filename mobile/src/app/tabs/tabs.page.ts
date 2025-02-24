import { ChangeDetectionStrategy, Component, inject, OnInit, viewChild } from '@angular/core';
import { IonTabs, IonTabBar, IonTabButton, IonIcon, ModalController } from '@ionic/angular/standalone';
import { Router } from '@angular/router';
import { FriendsStore } from '../friends/friends.store';
import { DefineExpenseComponent } from '../define-expense/define-expense.component';

@Component({
  selector: 'app-tabs',
  templateUrl: 'tabs.page.html',
  styleUrls: ['tabs.page.scss'],
  standalone: true,
  imports: [IonTabs, IonTabBar, IonTabButton, IonIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabsPage {
  readonly modalCtrl = inject(ModalController);
  readonly router = inject(Router);
  readonly friendsStore = inject(FriendsStore);
  constructor() {
  }

  async addNewExpense() {
    let selectedFriend = null;
    if (this.router.url.endsWith('tabs/friends')) {
      selectedFriend = this.friendsStore.selectedFriend();
    }
    const modal = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: { friend: selectedFriend }
    })
    modal.present();
  }
}
