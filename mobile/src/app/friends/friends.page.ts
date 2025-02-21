import { ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonButtons,
  IonIcon, ModalController, IonList, IonItem, IonAvatar, IonLabel,
} from '@ionic/angular/standalone';
import { FriendsStore } from './friends.store';
import { AddFriendComponent } from '../add-friend/add-friend.component';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-friends',
  templateUrl: 'friends.page.html',
  styleUrls: ['friends.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons, IonIcon, FormsModule, IonList, IonItem, IonAvatar, IonLabel, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FriendsPage implements OnInit {
  readonly friendsStore = inject(FriendsStore);
  readonly modalCtrl = inject(ModalController);
  constructor() {}

  async ngOnInit() {
    console.log('friends initialized...')
  }

  ionViewDidEnter() {
    console.log('User has entered the page');
  }

  ionViewDidLeave() {
    console.log('User has left the page');
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
