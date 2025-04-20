import { ChangeDetectionStrategy, Component, computed, inject, model, signal } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonIcon,
  IonButtons,
  IonList,
  IonItem,
  IonLabel, IonSelect, IonSelectOption, ModalController, ToastController, IonSpinner,
} from '@ionic/angular/standalone';
import { AuthStore } from '../login/auth.store';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { COUNTRIES_WITH_CALLING_CODES, CURRENCIES, DEFAULT_TOAST_DURATION } from '../constants';
import { FriendsStore } from '../friends/friends.store';
import { HelperService } from '../helper.service';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';
import { extractCountryCode, toInternationalPhone, toNationalPhone } from '../utility-functions';
import { PhonePipe } from '../pipes/phone.pipe';

@Component({
  selector: 'app-account',
  templateUrl: 'account.page.html',
  styleUrls: ['account.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons, IonList, IonItem, IonLabel, IonSelect, IonSelectOption, ReactiveFormsModule, FormsModule, PhoneWithCountryComponent, IonSpinner, PhonePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  defaultCurrency = model('');
  readonly authStore = inject(AuthStore);
  readonly friendsStore = inject(FriendsStore);
  readonly user = computed(() => this.authStore.user())
  readonly currencies = CURRENCIES;
  phoneInEditMode = signal(false);

  private formBuilder = inject(FormBuilder);
  private helperService = inject(HelperService);
  private modalCtrl = inject(ModalController);
  private toastCtrl = inject(ToastController);
  readonly loading = signal(false);
  public phoneForm = this.formBuilder.group({
    phoneNumber: this.formBuilder.nonNullable.control(toNationalPhone(this.user()?.phoneNumber || ''), [Validators.required]),
    country: this.formBuilder.nonNullable.control(extractCountryCode(this.user()?.phoneNumber || undefined)),
  })

  constructor() {
    this.authStore.loadUserData();
  }

  async savePhoneNumber() {
    if (this.phoneForm.valid) {
      try {
        this.loading.set(true);
        const phoneNumber = toInternationalPhone(
          this.phoneForm.get('phoneNumber')?.value!,
          this.phoneForm.get('country')?.value!
        );
        await this.authStore.updateUserData({
          phoneNumber,
        })
        this.phoneInEditMode.set(false);
        await this.modalCtrl.dismiss(phoneNumber, 'confirm')
      } catch (e) {
        await this.toastCtrl.create({
          message: 'Unable to save phone number. Please try later.',
          duration: DEFAULT_TOAST_DURATION
        });
      } finally {
        this.loading.set(false);
      }
    } else {
      this.phoneForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  async updateDefaultCurrency(value: string) {
    await this.authStore.updateUserData({
      currency: value,
      phoneNumber: this.user()!.phoneNumber || null,
      displayName: this.user()!.displayName,
    });
    this.friendsStore.loadFriends();
  }
}
