import { Component, computed, inject, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  IonAccordion, IonAccordionGroup,
  IonAvatar,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonNav,
  IonTitle,
  IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import { FriendWithBalance } from '../model';
import { TransactionDetailsComponent } from '../transaction-details/transaction-details.component';
import { FriendsStore } from '../friends.store';
import { shortName } from '../../utility-functions';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { DefineExpenseComponent, SplitOptions } from '../../define-expense/define-expense.component';
import { SettleUpComponent } from './settle-up/settle-up.component';
import { AddFriendComponent } from '../../add-friend/add-friend.component';

@Component({
  selector: 'app-friend-transactions',
  templateUrl: './friend-transactions.component.html',
  styleUrls: ['./friend-transactions.component.scss'],
  standalone: true,
  imports: [
    CurrencyPipe,
    IonAvatar,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonItem,
    IonLabel,
    IonList,
    IonTitle,
    IonToolbar,
    IonBackButton,
    ShortenNamePipe,
    DateFormatPipe,
    IonAccordion,
    IonAccordionGroup,
  ],
})
export class FriendTransactionsComponent {
  readonly friend = input.required<FriendWithBalance>();
  readonly nav = inject(IonNav);
  readonly friendsStore = inject(FriendsStore);
  readonly transactions = this.friendsStore.selectedTransactions;
  readonly modalCtrl = inject(ModalController);
  protected readonly shortName = shortName;
  readonly sortedOtherBalance = computed(() => this.friendsStore.selectedFriendBalance()?.other
    .sort((a, b) => b.amount - a.amount));

  constructor() {}

  openTransactionDetails(transaction: any) {
    this.nav.push(TransactionDetailsComponent, {
      transaction,
      friend: this.friend()
    });
  }

  async addExpense() {
    const modal = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: { friend: this.friend() }
    })
    modal.present();
  }

  ionViewWillEnter() {
    this.friendsStore.setSelectedFriend(this.friend());
  }

  async deleteFriend() {
    await this.friendsStore.deleteFriend(this.friend());
  }

  async editFriendInfo() {
    const modalInstance = await this.modalCtrl.create({
      component: AddFriendComponent,
      componentProps: {
        friend: this.friend(),
        isUpdating: true
      }
    })
    modalInstance.present();
  }

  async settleUp() {
    const modalInstance = await this.modalCtrl.create({
      component: SettleUpComponent,
      componentProps: {
        friend: this.friend()
      }
    })
    modalInstance.present();
  }
}
