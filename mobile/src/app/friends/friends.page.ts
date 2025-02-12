import { ChangeDetectionStrategy, Component, inject, OnInit, ViewChild } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonButtons,
  IonIcon, ModalController,
} from '@ionic/angular/standalone';
import { FriendsStore } from './friends.store';
import { AddFriendComponent } from '../add-friend/add-friend.component';
import { FormsModule } from '@angular/forms';
import { AuthStore } from '../login/auth.store';

@Component({
  selector: 'app-friends',
  templateUrl: 'friends.page.html',
  styleUrls: ['friends.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons, IonIcon, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FriendsPage implements OnInit {
  readonly friendsStore = inject(FriendsStore);
  readonly modalCtrl = inject(ModalController);
  readonly authStore = inject(AuthStore);
  constructor() {
    this.friendsStore.loadFriends()
  }

  async ngOnInit() {}

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
