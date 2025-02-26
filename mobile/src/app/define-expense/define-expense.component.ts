import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  Input,
  model,
  OnInit,
  signal,
} from '@angular/core';
import {
  ActionSheetController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput, IonItem, IonLabel, IonList, IonSelect, IonSelectOption, IonSpinner,
  IonTitle,
  IonToolbar, ModalController,
} from '@ionic/angular/standalone';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom, takeUntil, timer } from 'rxjs';
import { Router } from '@angular/router';
import { HelperService } from '../helper.service';
import { FriendsStore } from '../friends/friends.store';
import { Friend, Transaction } from '../friends/model';
import { SelectFriendComponent } from './select-friend/select-friend.component';
import { JsonPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { PRIVATE_API } from '../constants';
import { ShortenNamePipe } from '../pipes/shorten-name.pipe';
import { toObservable } from '@angular/core/rxjs-interop';
import { ComponentDestroyedMixin } from '../component-destroyed.mixin';

export enum SplitOptions {
  YouPaidSplitEqually = 'YouPaidSplitEqually',
  TheyPaidSplitEqually = 'TheyPaidSplitEqually',
  TheyOweYouAll = 'TheyOweYouAll',
  YouOweThemAll = 'YouOweThemAll'
}

@Component({
  selector: 'app-define-expense',
  templateUrl: './define-expense.component.html',
  styleUrls: ['./define-expense.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonInput,
    ReactiveFormsModule,
    IonContent,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonTitle,
    IonSpinner,
    IonButton,
    IonSelect,
    IonSelectOption,
    IonList,
    IonItem,
    IonBackButton,
    JsonPipe,
    IonLabel,
    ShortenNamePipe,
  ],
})
export class DefineExpenseComponent extends ComponentDestroyedMixin() implements OnInit {
  readonly friend = model<Friend|null>(null);
  readonly loading = signal(false);
  readonly isUpdating = input(false);
  readonly transaction = input<Transaction>();
  readonly http = inject(HttpClient);
  readonly modalCtrl = inject(ModalController);
  readonly helperService = inject<HelperService>(HelperService);
  readonly formBuilder = inject(FormBuilder);
  readonly actionSheetCtrl = inject(ActionSheetController);
  readonly friendsStore = inject(FriendsStore);
  readonly SplitOption = SplitOptions;
  readonly supportedCurrencies = ['PKR', 'USD', 'SGD'];
  readonly router = inject(Router);
  defineExpenseForm = this.formBuilder.group({
    description: this.formBuilder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]),
    currency: this.formBuilder.nonNullable.control('PKR', [Validators.required]),
    amount: this.formBuilder.nonNullable.control(null, [Validators.required, Validators.min(1)]),
    type: this.formBuilder.nonNullable.control({ value: SplitOptions.YouPaidSplitEqually, disabled: !this.friend() }, [Validators.required]),
  });
  isOwed = () => {
    return [SplitOptions.YouPaidSplitEqually, SplitOptions.TheyOweYouAll]
      .includes(this.defineExpenseForm.value.type!);
  };
  constructor() {
    super();

    toObservable(this.friend)
      .pipe(
        takeUntil(this.componentDestroyed)
      )
      .subscribe(value => {
        if (!value) {
          this.defineExpenseForm.get('type')?.disable();
        } else {
          this.defineExpenseForm.get('type')?.enable();
        }
      })
  }

  async ngOnInit() {
    if (!this.friend()) {
      const role = await this.chooseFriend();
      if (role !== 'confirm') {
        setTimeout(() => {
          this.closePopup();
        }, 1000)
      }
    }
  }

  canDismiss = async () => {
    const actionSheet = await this.actionSheetCtrl.create({
      header: 'Are you sure?',
      buttons: [
        {
          text: 'Yes',
          role: 'confirm',
        },
        {
          text: 'No',
          role: 'cancel',
        },
      ],
    });
    actionSheet.present();
    const { role } = await actionSheet.onWillDismiss();
    return role === 'confirm';
  };

  async closePopup() {
    if (!this.defineExpenseForm.dirty || await this.canDismiss()) {
      this.modalCtrl.dismiss();
    }
  }

  async onSubmit() {
    if (!this.friend()) {
      await this.helperService.showToast('Please select a friend first.');
      return;
    }
    if (this.defineExpenseForm.valid) {
      try {
        this.loading.set(true);
        await this.saveExpense(this.defineExpenseForm.getRawValue());
        let postSaveActions = [this.friendsStore.loadFriends()];
        console.log(this.friendsStore.selectedFriend());
        if (
          this.friendsStore.selectedFriend()
          && this.friend()?.friendId === this.friendsStore.selectedFriend()?.friendId
        ) {
          postSaveActions.push(this.friendsStore.loadSelectedTransactions())
        }
        await Promise.all(postSaveActions);
        this.modalCtrl.dismiss(null, 'confirm');
      } catch (e) {
        console.log(e);
        await this.helperService.showToast('Unable to add friend at the moment');
      } finally {
        this.loading.set(false);
      }
    } else {
      this.defineExpenseForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  async chooseFriend() {
    if (this.isUpdating()) {
      return;
    }
    const modal = await this.modalCtrl.create({
      component: SelectFriendComponent,
    })
    modal.present();

    const { data, role } = await modal.onWillDismiss();
    if (role === 'confirm') {
      this.friend.set(data['friend']);
    }
    return role;
  }

  async saveExpense(formValue: {
    description: string,
    currency: string,
    amount: number|null,
    type: SplitOptions
  }) {
    if (this.isUpdating()) {
      return firstValueFrom(
        this.http.put(`${PRIVATE_API}/transactions/update/transactionId/${this.transaction()?.transactionId}`, {
          ...formValue,
        })
      );
    } else {
      return firstValueFrom(this.http.post(`${PRIVATE_API}/transactions/add`, {
        ...formValue,
        recipientId: this.friend()!.friendId
      }));
    }
  }
}
