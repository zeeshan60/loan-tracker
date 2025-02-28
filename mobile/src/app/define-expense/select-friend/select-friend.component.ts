import { ChangeDetectionStrategy, Component, computed, inject, input, model, OnInit, signal } from '@angular/core';
import {
  IonAvatar, IonButton, IonButtons, IonChip, IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonSearchbar, IonTitle, IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { FriendsStore } from '../../friends/friends.store';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendWithBalance } from '../../friends/model';
import { DefineExpenseService } from '../define-expense.service';

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
    IonHeader, IonToolbar, IonTitle, IonContent, IonIcon, FormsModule, IonList, IonItem, IonAvatar, IonLabel, CurrencyPipe, IonSearchbar, IonButton, IonButtons, IonChip,
  ],
})
export class SelectFriendComponent {
  modalCtrl = inject(ModalController);
  friendsStore = inject(FriendsStore);
  defineExpenseService = inject(DefineExpenseService);
  friend = input<FriendWithBalance>();
  filter = model<string>('');
  readonly friends = computed(() => this.friendsStore.friends().filter(friend =>
    friend.name.toLowerCase().includes(this.filter().toLowerCase())
  ))
  constructor() {
  }

  friendBalanceColor(friend: FriendWithBalance) {
    if (!friend.mainBalance) {
      return 'light';
    }
    return friend.mainBalance.isOwed ? 'success' : 'danger';
  }

  async chooseFriend(friend: FriendWithBalance) {
    this.defineExpenseService.selectFriendModalInstance ?
      this.defineExpenseService.selectFriendModalInstance.dismiss({friend}, 'confirm') :
      this.modalCtrl.dismiss({friend}, 'confirm');
  }

  closePopup() {
    this.defineExpenseService.selectFriendModalInstance ?
      this.defineExpenseService.selectFriendModalInstance.dismiss() :
      this.modalCtrl.dismiss();
  }

  async createNewFriend() {
    const modal = await this.modalCtrl.create({
      component: AddFriendComponent,
      componentProps: { name: this.filter()}
    })
    modal.present();
    const { data: friend, role } = await modal.onWillDismiss();
    if (role === 'confirm') {
      this.chooseFriend(friend);
    }
  }
}
