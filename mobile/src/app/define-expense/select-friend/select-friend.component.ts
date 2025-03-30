import { ChangeDetectionStrategy, Component, computed, inject, input, model, OnInit, signal } from '@angular/core';
import {
  IonAvatar, IonButton, IonButtons, IonChip, IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList, IonListHeader,
  IonSearchbar, IonTitle, IonToolbar, LoadingController,
  ModalController,
} from '@ionic/angular/standalone';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { FriendsStore } from '../../friends/friends.store';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendWithBalance } from '../../friends/model';
import { DefineExpenseService } from '../define-expense.service';
import { isMobile } from '../../utils';
import { Contacts } from '@capacitor-community/contacts';

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
    IonHeader, IonToolbar, IonTitle, IonContent, IonIcon, FormsModule, IonList, IonItem, IonAvatar, IonLabel, CurrencyPipe, IonSearchbar, IonButton, IonButtons, IonChip, IonListHeader,
  ],
})
export class SelectFriendComponent {
  modalCtrl = inject(ModalController);
  friendsStore = inject(FriendsStore);
  defineExpenseService = inject(DefineExpenseService);
  loadingCtrl = inject(LoadingController);
  friend = input<FriendWithBalance>();
  filter = model<string>('');
  readonly friends = computed(() => this.friendsStore.friends().filter(friend =>
    friend.name.toLowerCase().includes(this.filter().toLowerCase())
  ))
  contacts = signal<{name: string, phoneNumber: string}[]>([]);
  readonly filteredContacts = computed(() => {
    return this.contacts().filter((contact) => {
      return contact.name.toLowerCase().includes(this.filter().toLowerCase()) || contact.phoneNumber.includes(this.filter().toLowerCase())
    })
  })
  protected readonly isMobile = isMobile;

  constructor() {
    if (isMobile) {
      Contacts.getContacts({
        projection: {
          name: true,
          phones: true,
        },
      }).then((result) => {
        let mappedContacts = [];
        for (const contact of result.contacts) {
          const number = contact.phones?.[0]?.number;
          if (number) {
            mappedContacts.push({ name: contact.name?.display || '', phoneNumber: number })
          }
        }
        this.contacts.set(mappedContacts);
      })
    }

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

  async chooseContact(contact: { name: string, phoneNumber: string}) {
    const loader = await this.loadingCtrl.create();
    await loader.present();
    try {
      const friend = await this.friendsStore.addFriend({
        name: contact.name,
        phoneNumber: contact.phoneNumber,
        email: null
      });
      this.chooseFriend(friend);
    } catch (e) {} finally {
      await loader.dismiss()
    }
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
