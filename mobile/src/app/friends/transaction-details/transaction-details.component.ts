import { Component, computed, inject, input, model } from '@angular/core';
import {
  IonAvatar,
  IonBackButton, IonButton,
  IonButtons,
  IonContent,
  IonHeader, IonIcon, IonItem, IonLabel,
  IonList, IonNav,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { FriendWithBalance, HistoryChangeTypeEnum, Transaction } from '../model';
import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DefineExpenseComponent, SplitOption, SplitOptionsEnum } from '../../define-expense/define-expense.component';
import { FriendsStore } from '../friends.store';
import { HelperService } from '../../helper.service';
import { ModalService } from '../../modal.service';
import { isSettlement } from '../../utility-functions';
import { SettleUpComponent } from '../friend-transactions/settle-up/settle-up.component';

const historyChangeType = {
  [HistoryChangeTypeEnum.DESCRIPTION]: "description",
  [HistoryChangeTypeEnum.TOTAL_AMOUNT]: "total amount",
  [HistoryChangeTypeEnum.CURRENCY]: "currency",
  [HistoryChangeTypeEnum.SPLIT_TYPE]: "split type",
  [HistoryChangeTypeEnum.DELETED]: "deleted",
  [HistoryChangeTypeEnum.TRANSACTION_DATE]: "transaction date"
}

@Component({
    selector: 'mr-transaction-details',
    templateUrl: './transaction-details.component.html',
    styleUrls: ['./transaction-details.component.scss'],
    standalone: true,
  imports: [
    IonBackButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonList,
    IonTitle,
    IonToolbar,
    IonLabel,
    CurrencyPipe,
    IonAvatar,
    IonIcon,
    IonItem,
    ShortenNamePipe,
    IonButton,
    DatePipe,
    NgClass,
  ],
})
export class TransactionDetailsComponent {
  readonly SplitOptions = SplitOptionsEnum;
  readonly friendsStore = inject(FriendsStore);
  readonly helperService = inject(HelperService);
  readonly modalService = inject(ModalService);
  readonly nav = inject(IonNav);
  readonly transactionId = input<string>();
  readonly transaction = input<Transaction>();
  readonly friend = input.required<FriendWithBalance>();
  readonly isSettlement = (splitType: SplitOption) => (
    [SplitOptionsEnum.TheyPaidToSettle, SplitOptionsEnum.YouPaidToSettle] as SplitOption[]
  ).includes(splitType)
  protected readonly SplitOptionsEnum = SplitOptionsEnum;
  readonly latestTransaction = computed(() => {
    if (this.transaction()) {
      return this.transaction();
    }
    let found: Transaction | undefined;
    this.friendsStore.selectedTransactions().some((transactionsByMonth) => {
      return transactionsByMonth.transactions.some((transaction) => {
        if (transaction.transactionId === this.transactionId()) {
          found = transaction;
          return true;
        }
        return false;
      })
    })
    return found;
  })
  protected readonly historyChangeType = historyChangeType;
  protected readonly HistoryChangeType = HistoryChangeTypeEnum;
  protected splitOptionsText : {[key: string]: string} = {
    [SplitOptionsEnum.YouPaidSplitEqually]: 'You paid split equally',
    [SplitOptionsEnum.TheyPaidSplitEqually]: 'They paid split equally',
    [SplitOptionsEnum.TheyOweYouAll]: 'They owe you all',
    [SplitOptionsEnum.YouOweThemAll]: 'You owe them all',
    [SplitOptionsEnum.TheyPaidToSettle]: 'They paid to settle',
    [SplitOptionsEnum.YouPaidToSettle]: 'You paid to settle',
  }

  constructor() {
  }

  async deleteTransaction() {
    const response = await this.helperService.showConfirmAlert()
    if (response.role === 'confirm') {
      try {
        await this.friendsStore.deleteTransaction(this.transactionId() || this.transaction()?.transactionId);
        this.nav.pop();
      } catch (e) {}
    }
  }

  async updateTransaction() {
    if(isSettlement(this.latestTransaction())) {
      this.modalService.showModal({
        component: SettleUpComponent,
        componentProps: {
          friend: this.friend(),
          transaction: this.latestTransaction(),
        }
      })
    } else {
      this.modalService.showModal({
        component: DefineExpenseComponent,
        componentProps: {
          friend: this.friend(),
          isUpdating: true,
          transaction: this.latestTransaction(),
        }
      })
    }
  }
}
