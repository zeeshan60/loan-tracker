import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  IonAvatar,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel, IonList, IonNav, IonTitle, IonToolbar, ModalController,
} from '@ionic/angular/standalone';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { FriendsStore } from '../friends.store';
import { FormsModule } from '@angular/forms';
import { Friend } from '../model';
import { FriendTransactionsComponent } from '../friend-transactions/friend-transactions.component';

@Component({
  selector: 'app-list-friends',
  templateUrl: './list-friends.component.html',
  styleUrls: ['./list-friends.component.scss'],
  standalone: true,
  imports: [
    IonHeader,
    IonToolbar,
    IonTitle,
    IonContent,
    IonButton,
    IonButtons,
    IonIcon,
    FormsModule,
    IonList,
    IonItem,
    IonAvatar,
    IonLabel,
    CurrencyPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListFriendsComponent  implements OnInit {
  readonly friendsStore = inject(FriendsStore);
  readonly modalCtrl = inject(ModalController);
  readonly nav = inject(IonNav);
  constructor() {}

  ngOnInit() {}

  async chooseFriend(friend: Friend) {
    this.nav.push(FriendTransactionsComponent, { friend });
  }

  async addFriend() {
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
