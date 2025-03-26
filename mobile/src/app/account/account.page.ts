import { ChangeDetectionStrategy, Component, computed, inject, model, signal } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonIcon,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonList,
  IonItem,
  IonLabel, IonSelect, IonSelectOption,
} from '@ionic/angular/standalone';
import { AuthStore } from '../login/auth.store';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CURRENCIES } from '../constants';

@Component({
  selector: 'app-account',
  templateUrl: 'account.page.html',
  styleUrls: ['account.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel, IonSelect, IonSelectOption, ReactiveFormsModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  defaultCurrency = model('');
  readonly authStore = inject(AuthStore);
  readonly user = computed(() => this.authStore.user())
  readonly currencies = CURRENCIES;
  constructor() {
    this.authStore.loadUserData();
  }

  async updateDefaultCurrency(value: string) {
    await this.authStore.updateUserData({
      currency: value,
      phoneNumber: this.user()!.phoneNumber || null,
      displayName: this.user()!.displayName,
    });
  }
}
