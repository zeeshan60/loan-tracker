import { ChangeDetectionStrategy, Component, computed, inject, input, model, OnInit, signal } from '@angular/core';
import {
  IonAvatar, IonButtons, IonChip, IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList, IonListHeader,
  IonSearchbar, IonTitle, IonToolbar, LoadingController,
} from '@ionic/angular/standalone';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { FriendsStore } from '../../friends/friends.store';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendWithBalance } from '../../friends/model';
import { DefineExpenseService } from '../define-expense.service';
import { isMobile } from '../../utils';
import { Contacts } from '@capacitor-community/contacts';
import { ModalIndex, ModalService } from '../../modal.service';

@Component({
  selector: 'mr-select-friend',
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
    IonHeader, IonToolbar, IonTitle, IonContent, IonIcon, FormsModule, IonList, IonItem, IonAvatar, IonLabel, CurrencyPipe, IonSearchbar, IonButtons, IonChip, IonListHeader,
  ],
})
export class SelectFriendComponent {
  modalIndex = input.required<ModalIndex>()
  friendsStore = inject(FriendsStore);
  defineExpenseService = inject(DefineExpenseService);
  modalService = inject(ModalService);
  loadingCtrl = inject(LoadingController);
  friend = input<FriendWithBalance>();
  context = input<'ChooseFriend' | 'AddFriend'>('ChooseFriend');
  filter = model<string>('');
  readonly friends = computed(() => this.friendsStore.friends().filter(friend =>
    friend.name.toLowerCase().includes(this.filter().toLowerCase()),
  ))
  contacts = signal<{ name: string, phoneNumber: string }[]>([]);
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
            mappedContacts.push({name: contact.name?.display || '', phoneNumber: number})
          }
        }
        this.contacts.set(mappedContacts);
      })
    }

  }

  async chooseFriend(friend: FriendWithBalance) {
    this.modalService.dismiss(this.modalIndex(), friend, 'confirm');
  }

  async chooseContact(contact: { name: string, phoneNumber: string }) {
    const loader = await this.loadingCtrl.create();
    await loader.present();
    try {
      const friend = await this.friendsStore.addFriend({
        name: contact.name,
        phoneNumber: contact.phoneNumber,
        email: null,
      });
      this.chooseFriend(friend);
    } catch (e) {
    } finally {
      await loader.dismiss()
    }
  }

  closePopup() {
    this.modalService.dismiss(this.modalIndex());
  }

  async createNewFriend() {
    const modalIndex = await this.modalService.showModal({
      component: AddFriendComponent,
      componentProps: {name: this.filter()},
    })
    const {data: friend, role} = await this.modalService.onWillDismiss<FriendWithBalance>(modalIndex);
    if (role === 'confirm') {
      this.chooseFriend(friend);
    }
  }
}
