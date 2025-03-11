import { ChangeDetectionStrategy, Component, inject, model, signal } from '@angular/core';
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
import { HelperService } from '../helper.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CURRENCIES } from '../constants';

type Profile = {
  displayName: string,
  email: string,
  photoURL: string,
  phoneNumber: string|null,
}

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
  readonly helperService = inject(HelperService);
  readonly authStore = inject(AuthStore);
  readonly user = signal<Profile | null>(null)
  readonly currencies = CURRENCIES;
  constructor() {
    this.helperService.getUser().then((user) => {
      this.user.set({
        displayName: user?.displayName!,
        email: user?.email!,
        photoURL: user?.photoURL!,
        phoneNumber: user?.phoneNumber || null,
      });
    })
  }
}
