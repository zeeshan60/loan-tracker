import { ChangeDetectionStrategy, Component, inject, ViewChild } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonButtons,
  IonIcon, ModalController,
} from '@ionic/angular/standalone';
import { FriendsStore } from '../store/friends.store';
import { AddFriendComponent } from '../add-friend/add-friend.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-friends',
  templateUrl: 'friends.page.html',
  styleUrls: ['friends.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons, IonIcon, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FriendsPage {
  readonly friendsStore = inject(FriendsStore);
  readonly modalCtrl = inject(ModalController);
  constructor() {
    this.friendsStore.loadFriends();
  }

  async addFriend() {
    const modal = await this.modalCtrl.create({
      component: AddFriendComponent,
    })
    modal.present();
    const { data, role } = await modal.onWillDismiss();
    if (role === 'confirm') {
      this.friendsStore.loadFriends();
    }
  }
}
