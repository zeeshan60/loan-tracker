import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrencyPipe, NgOptimizedImage } from '@angular/common';
import {
  IonAvatar,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel, IonList, IonNav, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import { FriendsStore } from '../friends.store';
import { FormsModule } from '@angular/forms';
import { FriendWithBalance } from '../model';
import { FriendTransactionsComponent } from '../friend-transactions/friend-transactions.component';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { SelectFriendComponent } from '../../define-expense/select-friend/select-friend.component';
import { isMobile } from '../../utils';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { OverallBalanceComponent } from '../overall-balance/overall-balance.component';
import { ModalService } from '../../modal.service';

@Component({
  selector: 'mr-list-friends',
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
    OverallBalanceComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListFriendsComponent {
  readonly friendsStore = inject(FriendsStore);
  readonly nav = inject(IonNav);
  readonly modalService = inject(ModalService);

  constructor() {}

  async chooseFriend(friend: FriendWithBalance) {
    this.friendsStore.setSelectedFriend(friend.friendId);
    this.nav.push(FriendTransactionsComponent);
  }

  ionViewDidEnter() {
    this.friendsStore.setSelectedFriend(null);
  }

  async addFriend() {
    if (isMobile) {
      await this.modalService.showModal({
        component: SelectFriendComponent,
        componentProps: {
          context: 'AddFriend'
        }
      })
    } else {
      await this.modalService.showModal({
        component: AddFriendComponent,
      })
    }
  }
}
