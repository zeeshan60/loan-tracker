import { ChangeDetectionStrategy, Component, inject, input, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonList,
  IonSpinner, IonTitle, IonToolbar, ModalController, ToastController,
} from '@ionic/angular/standalone';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';
import { HelperService } from '../helper.service';
import { AuthStore, Region, User } from '../login/auth.store';
import { toInternationalPhone } from '../utility-functions';
import { COUNTRIES_WITH_CALLING_CODES, DEFAULT_TOAST_DURATION } from '../constants';

@Component({
  selector: 'app-ask-for-phone',
  templateUrl: './ask-for-phone.component.html',
  styleUrls: ['./ask-for-phone.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonInput,
    IonItem,
    IonList,
    IonSpinner,
    IonTitle,
    IonToolbar,
    PhoneWithCountryComponent,
    ReactiveFormsModule,
  ],
})
export class AskForPhoneComponent  implements OnInit {
  readonly user = input.required<User>();
  readonly region = input.required<Region>();
  readonly authStore = inject(AuthStore);
  private formBuilder = inject(FormBuilder);
  private helperService = inject(HelperService);
  private modalCtrl = inject(ModalController);
  private toastCtrl = inject(ToastController);
  readonly loading = signal(false);
  public phoneForm = this.formBuilder.group({
    phoneNumber: this.formBuilder.nonNullable.control('', [Validators.required]),
    country: this.formBuilder.nonNullable.control(COUNTRIES_WITH_CALLING_CODES[0].code),
  })
  constructor() { }

  ngOnInit() {
    this.preSetCountryCode();
  }

  private preSetCountryCode() {
    const availableCountries = COUNTRIES_WITH_CALLING_CODES.map(country => country.code);
    if (availableCountries.includes(this.region()?.country_code)) {
      this.phoneForm.get('country')?.setValue(this.region()?.country_code);
    }
  }

  cancel() {
    this.modalCtrl.dismiss(null, 'cancel');
  }

  async onSubmit() {
    if (this.phoneForm.valid) {
      try {
        this.loading.set(true);
        const phoneNumber = toInternationalPhone(
          this.phoneForm.get('phoneNumber')!.value,
          this.phoneForm.get('country')!.value
        );
        await this.authStore.updateUserData({
          currency: this.user().currency, // todo: remove this once api is fixed
          displayName: this.user().displayName, // todo: remove this once api is fixed
          phoneNumber,
        })
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
}
