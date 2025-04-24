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
    OverallBalanceComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListFriendsComponent  implements OnInit {
  readonly friendsStore = inject(FriendsStore);
  readonly nav = inject(IonNav);
  readonly modalService = inject(ModalService);

  constructor() {}

  ngOnInit() {}

  async chooseFriend(friend: FriendWithBalance) {
    this.nav.push(FriendTransactionsComponent, { friend });
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
