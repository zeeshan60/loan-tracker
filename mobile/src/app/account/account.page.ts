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
  IonLabel,
  ToastController,
  IonSpinner, LoadingController,
} from '@ionic/angular/standalone';
import { AuthStore } from '../login/auth.store';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CURRENCIES, DEFAULT_TOAST_DURATION, PRIVATE_API } from '../constants';
import { FriendsStore } from '../friends/friends.store';
import { HelperService } from '../helper.service';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';
import { extractCountryCode, toInternationalPhone, toNationalPhone } from '../utility-functions';
import { PhonePipe } from '../pipes/phone.pipe';
import { CurrenciesDropdownComponent } from '../currencies-dropdown/currencies-dropdown.component';
import { HttpClient } from '@angular/common/http';
import { getAuth } from '@angular/fire/auth';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'mr-account',
  templateUrl: 'account.page.html',
  styleUrls: ['account.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons, IonList, IonItem, IonLabel, ReactiveFormsModule, FormsModule, PhoneWithCountryComponent, IonSpinner, PhonePipe, CurrenciesDropdownComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  defaultCurrency = model('');
  readonly loadingCtrl = inject(LoadingController);
  readonly http = inject(HttpClient);
  readonly authStore = inject(AuthStore);
  readonly friendsStore = inject(FriendsStore);
  readonly user = computed(() => this.authStore.user())
  readonly userCurrency = computed(() => {
    return CURRENCIES.find(currency => currency.code === this.user()!.currency)
  });
  phoneInEditMode = signal(false);
  private formBuilder = inject(FormBuilder);
  private helperService = inject(HelperService);
  private toastCtrl = inject(ToastController);
  readonly loading = signal(false);
  public phoneForm = this.formBuilder.group({})
  readonly selectedPhone = computed(() => ({
    phoneNumber: this.user()?.phoneNumber,
    country: extractCountryCode(this.user()?.phoneNumber)
  }))

  constructor() {
    this.authStore.loadUserData();
  }

  async savePhoneNumber() {
    if (this.phoneForm.valid) {
      try {
        this.loading.set(true);
        const phoneNumber = toInternationalPhone(
          this.phoneForm.get('phone.phoneNumber')?.value!,
          this.phoneForm.get('phone.country')?.value!
        );
        await this.authStore.updateUserData({
          phoneNumber,
        })
        this.phoneInEditMode.set(false);
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

  public async updateDefaultCurrency(value: string) {
    await this.authStore.updateUserData({
      currency: value,
      phoneNumber: this.user()!.phoneNumber || null,
      displayName: this.user()!.displayName,
    });
    this.friendsStore.loadFriends();
    this.friendsStore.loadSelectedTransactions();
  }

  async deleteUserAccount(): Promise<void> {
    const confirmation = await this.helperService.showConfirmAlert(
      `You are about to delete your account and data. This action cannot be undone. Do you wish to proceed?`, 'Let\'s do it'
    )
    if (confirmation.role !== 'confirm') return;

    const loader = await this.loadingCtrl.create();
    loader.present();
    try {
      await firstValueFrom(this.http.delete(`${PRIVATE_API}/users`));
    } catch (e) {
      let toast = await this.toastCtrl.create({
        message: 'Unable to delete user at the moment. Please login and try again.',
        duration: DEFAULT_TOAST_DURATION,
        color: 'danger'
      });
      toast.present();
    } finally {
      await this.authStore.signOut();
      await loader.dismiss();
    }
  }


  ionViewWillEnter() {
    if (!this.user()?.currency) {
      this.authStore.fetchAndSaveUserData();
    }
  }
}
