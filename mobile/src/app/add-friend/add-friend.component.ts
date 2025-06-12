import {
  ChangeDetectionStrategy,
  Component,
  inject, input,
  Input,
  OnInit,
  signal,
} from '@angular/core';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput, IonItem, IonList,
  IonSpinner,
  IonTitle,
  IonToolbar,
  IonLabel,
  IonBackButton,
  IonIcon,
} from '@ionic/angular/standalone';
import {
  AbstractControl,
  FormBuilder, FormControl, FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators,
} from '@angular/forms';
import { HelperService } from '../helper.service';
import { FriendsStore } from '../friends/friends.store';
import { FriendWithBalance } from '../friends/model';
import { extractCountryCode, toInternationalPhone, toNationalPhone } from '../utility-functions';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';
import { COUNTRIES_WITH_CALLING_CODES } from '../constants';
import { ModalIndex, ModalService } from '../modal.service';

@Component({
  selector: 'mr-add-friend',
  templateUrl: './add-friend.component.html',
  styleUrls: ['./add-friend.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    IonInput,
    IonToolbar,
    IonHeader,
    IonButton,
    IonButtons,
    IonLabel,
    IonTitle,
    IonContent,
    IonSpinner,
    ReactiveFormsModule,
    IonItem, IonList, PhoneWithCountryComponent,
    IonBackButton,
    IonIcon,
  ]
})
export class AddFriendComponent implements OnInit {
  @Input() name: string = '';
  readonly friend = input<FriendWithBalance|null>(null);
  readonly isUpdating = input(false);
  readonly modalIndex = input.required<ModalIndex>();
  readonly friendsStore = inject(FriendsStore);
  private formBuilder = inject(FormBuilder);
  private helperService = inject(HelperService);
  private modalService = inject(ModalService);
  readonly loading = signal(false);
  public addFriendForm = this.formBuilder.group({
    name: this.formBuilder.nonNullable.control('', [Validators.required]),
    email: new FormControl<string|null>(null, [Validators.email]),
  }, {
    validators: [(control: AbstractControl): ValidationErrors | null => {
      const email = control.get('email') as AbstractControl;
      const phone = control.get('phone') as FormGroup;

      if (!email.value && !phone?.get('phoneNumber').value) {
        return { noContactInformation: true };
      }

      return null;
    }]
  })

  constructor() {
  }

  getSelectedPhoneValue() {
    return {
      phoneNumber: toNationalPhone(this.friend()?.phone!),
      country: extractCountryCode(this.friend()?.phone!) || COUNTRIES_WITH_CALLING_CODES[0].code
    }
  }

  ngOnInit() {
    if (this.isUpdating()) {
      this.addFriendForm.patchValue({
        name: this.friend()?.name,
        email: this.friend()?.email,
      })
    } else {
      this.addFriendForm.patchValue({
        name: this.name,
      })
    }
  }

  cancel() {
    this.modalService.dismiss(this.modalIndex());
  }

  async onSubmit() {
    if (this.addFriendForm.valid) {
      try {
        this.loading.set(true);
        const mappedValue = {
          name: this.addFriendForm.get('name')!.value,
          email: this.addFriendForm.get('email')!.value || null,
          phoneNumber: toInternationalPhone(this.addFriendForm.get('phone.phoneNumber')!.value, this.addFriendForm.get('phone.country')!.value) || null,
        }
        const friend = this.isUpdating() ?
          await this.friendsStore.updateFriend(this.friend()!.friendId, mappedValue) :
          await this.friendsStore.addFriend(mappedValue);
        await this.modalService.dismiss(this.modalIndex(), friend, 'confirm')
      } finally {
        this.loading.set(false);
      }
    } else {
      this.addFriendForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }
}
