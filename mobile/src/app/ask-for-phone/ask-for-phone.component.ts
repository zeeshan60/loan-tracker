import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonItem,
  IonList,
  IonSpinner, IonToolbar, ToastController,
} from '@ionic/angular/standalone';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';
import { HelperService } from '../helper.service';
import { AuthStore, Region, User } from '../login/auth.store';
import { toInternationalPhone } from '../utility-functions';
import { COUNTRIES_WITH_CALLING_CODES, DEFAULT_TOAST_DURATION } from '../constants';
import { ModalIndex, ModalService } from '../modal.service';

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
    IonItem,
    IonList,
    IonSpinner,
    IonToolbar,
    PhoneWithCountryComponent,
    ReactiveFormsModule,
  ],
})
export class AskForPhoneComponent  implements OnInit {
  readonly region = input.required<Region>();
  readonly modalIndex = input.required<ModalIndex>();
  readonly authStore = inject(AuthStore);
  private formBuilder = inject(FormBuilder);
  private helperService = inject(HelperService);
  private modalService = inject(ModalService);
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
    this.modalService.dismiss(this.modalIndex());
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
          phoneNumber,
        })
        await this.modalService.dismiss(this.modalIndex(), phoneNumber, 'confirm')
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
