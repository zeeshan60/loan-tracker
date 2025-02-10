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
import { UserService } from '../user.service';

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
  readonly userService = inject(UserService);
  readonly modalCtrl = inject(ModalController);
  constructor() {
    this.friendsStore.loadFriends()
  }

  async ngOnInit() {
    console.log(await this.userService.getUser()?.getIdToken());
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
