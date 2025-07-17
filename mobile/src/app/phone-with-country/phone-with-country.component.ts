import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { COUNTRIES_WITH_CALLING_CODES, PHONE_MASKS } from '../constants';
import {
  AbstractControl, ControlContainer, FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn, Validators,
} from '@angular/forms';
import { IonInput } from '@ionic/angular/standalone';
import { isPhoneNumberValid, toInternationalPhone, toNationalPhone } from '../utility-functions';
import { MaskitoElementPredicate } from '@maskito/core';
import { MaskitoDirective } from '@maskito/angular';
import { ModalService } from '../modal.service';
import { CountriesDropdownComponent } from '../countries-dropdown/countries-dropdown.component';

@Component({
  selector: 'mr-phone-with-country',
  templateUrl: './phone-with-country.component.html',
  styleUrls: ['./phone-with-country.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    IonInput,
    ReactiveFormsModule,
    MaskitoDirective,
    CountriesDropdownComponent,
  ],
  viewProviders: [
    { provide: ControlContainer, useFactory: () => inject(ControlContainer, { skipSelf: true })}
  ]
})
export class PhoneWithCountryComponent  implements OnInit {
  readonly canShowLabels = input<boolean>(true);
  readonly isRequired = input<boolean>(false);
  readonly selectedValue = input<{phoneNumber?: string, country?: string}>()
  readonly fb = inject(FormBuilder);
  readonly parentGroupContainer = inject(ControlContainer);
  get parentFormGroup() {
    return this.parentGroupContainer.control as FormGroup;
  }
  readonly group = input<FormGroup>();
  readonly phoneNumberValidator = (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value || !this.parentFormGroup) {
        return null;
      }

      return isPhoneNumberValid(toInternationalPhone(value, this.parentFormGroup!.get('phone.country')!.value))
        ? null
        : { invalidPhone: true };
    };
  }
  readonly phoneMasks = PHONE_MASKS;
  readonly maskPredicate: MaskitoElementPredicate = async (el) => (el as HTMLIonInputElement).getInputElement();
  countries = COUNTRIES_WITH_CALLING_CODES;
  selectedCountryCode = signal<string>(this.parentFormGroup?.get('phone.country')?.value)
  selectedCountry = computed(() => this.countries.find(
    country => country.code === this.selectedCountryCode()
  ));
  phoneMask = computed(() => this.selectedCountryCode() ? this.phoneMasks[this.selectedCountryCode()] : {});
  selectedCountryPlaceholder = computed(() => this.selectedCountry()?.placeholder || '');

  modalService = inject(ModalService);

  constructor() { }

  getErrorTextForPhone() {
    if (this.parentFormGroup?.get('phone.phoneNumber').hasError('invalidPhone')) {
      return 'Invalid phone number.'
    } else if (this.parentFormGroup?.get('phone.phoneNumber').hasError('required')) {
      return 'Phone is required'
    } else {
      return 'Invalid';
    }
  }

  ngOnInit() {
    const initialSelectedCountry = this.selectedValue()?.country || COUNTRIES_WITH_CALLING_CODES[0].code;
    this.selectedCountryCode.set(initialSelectedCountry);
    this.parentFormGroup.addControl('phone', this.fb.group({
      phoneNumber: this.fb.nonNullable.control(toNationalPhone(this.selectedValue()?.phoneNumber) || '', this.isRequired() ? [Validators.required] : []),
      country: this.fb.nonNullable.control(initialSelectedCountry)
    }));

    this.parentFormGroup!.get('phone.country')!.valueChanges
      .subscribe((value: string) => {
        this.selectedCountryCode.set(value);
        this.parentFormGroup!.get('phone.phoneNumber')?.setValue('');
        this.parentFormGroup!.get('phone.phoneNumber')?.updateValueAndValidity();
      });

    const phoneNumberControl = this.parentFormGroup!.get('phone.phoneNumber');
    if (phoneNumberControl) {
      phoneNumberControl.addValidators([
        this.phoneNumberValidator()
      ]);
      phoneNumberControl.updateValueAndValidity();
    }
  }

  ngOnDestroy() {
    this.parentFormGroup.removeControl('phone');
  }
}
