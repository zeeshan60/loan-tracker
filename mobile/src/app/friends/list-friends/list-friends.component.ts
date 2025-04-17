import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  IonAccordion, IonAccordionGroup,
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
import { FriendWithBalance } from '../model';
import { FriendTransactionsComponent } from '../friend-transactions/friend-transactions.component';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { AuthStore } from '../../login/auth.store';

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
    CurrencyPipe,
    ShortenNamePipe,
    IonAccordion,
    IonAccordionGroup,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListFriendsComponent  implements OnInit {
  readonly friendsStore = inject(FriendsStore);
  readonly authStore = inject(AuthStore);
  readonly modalCtrl = inject(ModalController);
  readonly nav = inject(IonNav);
  readonly sortedOtherBalance = computed(() => this.friendsStore.overallBalance()?.other
    .sort((a, b) => b.amount - a.amount));
  constructor() {}

  ngOnInit() {}

  async chooseFriend(friend: FriendWithBalance) {
    this.nav.push(FriendTransactionsComponent, { friend });
  }

  ionViewDidEnter() {
    this.friendsStore.setSelectedFriend(null);
  }

  async addFriend() {
    const modal = await this.modalCtrl.create({
      component: AddFriendComponent,
    })
    modal.present();
  }
}
