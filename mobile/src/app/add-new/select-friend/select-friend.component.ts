import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { IonicModule, NavController } from '@ionic/angular';
import { DefineExpenseComponent } from '../define-expense/define-expense.component';
import {
  IonAvatar, IonButton, IonButtons, IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonNav, IonTitle, IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { FriendsStore } from '../../friends/friends.store';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Friend } from '../../friends/model';

@Component({
  selector: 'app-select-friend',
  templateUrl: './select-friend.component.html',
  styleUrls: ['./select-friend.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    IonAvatar,
    IonIcon,
    IonItem,
    IonLabel,
    IonList,

    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons, IonIcon, FormsModule, IonList, IonItem, IonAvatar, IonLabel, CurrencyPipe
  ],
})
export class SelectFriendComponent  implements OnInit {
  readonly nav = inject(IonNav);
  readonly defineExpenseComponent = DefineExpenseComponent;
  modalCtrl = inject(ModalController);
  friendsStore = inject(FriendsStore);
  constructor() { }

  ngOnInit() {}

  navigateToPageTwo(friend: Friend) {
    this.nav.push(DefineExpenseComponent, { friend });
  }

  async chooseFriend(friend: Friend) {
    this.navigateToPageTwo(friend);
  }

  async createNewFriend() {
    const modal = await this.modalCtrl.create({
      component: AddFriendComponent,
    })
    modal.present();
    const { role } = await modal.onWillDismiss();
    if (role === 'confirm') {
      this.friendsStore.loadFriends();
    }
  }
}
