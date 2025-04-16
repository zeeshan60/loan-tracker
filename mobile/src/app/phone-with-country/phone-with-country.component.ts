import { Component, computed, Input, input, OnInit, signal } from '@angular/core';
import { COUNTRIES_WITH_CALLING_CODES, PHONE_MASKS } from '../constants';
import {
  AbstractControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { IonInput, IonSelect, IonSelectOption } from '@ionic/angular/standalone';
import { NgForOf } from '@angular/common';
import { isValidPhoneNumber, toInternationalPhone } from '../utility-functions';
import { MaskitoElementPredicate } from '@maskito/core';
import { MaskitoDirective } from '@maskito/angular';

@Component({
    selector: 'app-phone-with-country',
    templateUrl: './phone-with-country.component.html',
    styleUrls: ['./phone-with-country.component.scss'],
    standalone: true,
  imports: [
    FormsModule,
    IonInput,
    IonSelect,
    IonSelectOption,
    NgForOf,
    ReactiveFormsModule,
    MaskitoDirective,
  ],
})
export class PhoneWithCountryComponent  implements OnInit {
  readonly group = input<FormGroup>();
  readonly phoneNumberValidator = (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value || !this.group()) {
        return null;
      }

      return isValidPhoneNumber(toInternationalPhone(value, this.group()!.get('country')!.value))
        ? null
        : { invalidPhone: true };
    };
  }
  readonly phoneMasks = PHONE_MASKS;
  readonly maskPredicate: MaskitoElementPredicate = async (el) => (el as HTMLIonInputElement).getInputElement();
  countries = COUNTRIES_WITH_CALLING_CODES;
  selectedCountryCodeChanges = signal('');
  selectedCountryCode = computed(() =>
    this.selectedCountryCodeChanges()
    || this.group()?.get('country')!.value
    || this.countries[0].code);
  selectedCountry = computed(() => this.countries.find(
    country => country.code === this.selectedCountryCode()
  ));
  selectedCountryText = computed(() => this.selectedCountry() ?
    `${this.selectedCountry()!.flag} ${this.selectedCountry()!.dialCode}` :
    ''
  );
  phoneMask = computed(() => this.selectedCountryCode() ? this.phoneMasks[this.selectedCountryCode()] : {});
  selectedCountryPlaceholder = computed(() => this.selectedCountry()?.placeholder || '');

  constructor() { }

  getErrorTextForPhone() {
    if (this.group()?.controls['phoneNumber'].hasError('invalidPhone')) {
      return 'Invalid phone number.'
    } else {
      return '';
    }
  }

  ngOnInit() {
    this.group()!.get('country')!.valueChanges.subscribe((value) => {
      this.selectedCountryCodeChanges.set(value);
      this.group()!.get('phoneNumber')?.setValue('');
      this.group()!.get('phoneNumber')?.updateValueAndValidity();
    });

    const phoneNumberControl = this.group()!.get('phoneNumber');
    if (phoneNumberControl) {
      phoneNumberControl.setValidators([
        this.phoneNumberValidator()
      ]);
      phoneNumberControl.updateValueAndValidity();
    }
  }
}
