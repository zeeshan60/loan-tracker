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
  IonInput, IonItem, IonList, IonSearchbar, IonSelect, IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import {
  FormBuilder, FormControl, FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { HelperService } from '../helper.service';
import { FriendsStore } from '../friends/friends.store';
import { FriendWithBalance } from '../friends/model';
import { extractCountryCode, toInternationalPhone, toNationalPhone } from '../utility-functions';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';
import { COUNTRIES_WITH_CALLING_CODES } from '../constants';

@Component({
  selector: 'app-add-friend',
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
    IonTitle,
    IonContent,
    IonSpinner,
    ReactiveFormsModule,
    IonItem, IonList, PhoneWithCountryComponent,
  ],
})
export class AddFriendComponent implements OnInit {
  @Input() name: string = '';
  readonly friend = input<FriendWithBalance|null>(null);
  readonly isUpdating = input(false);
  readonly friendsStore = inject(FriendsStore);
  private formBuilder = inject(FormBuilder);
  private modalCtrl = inject(ModalController);
  private helperService = inject(HelperService);
  readonly loading = signal(false);
  public addFriendForm = this.formBuilder.group({
    name: this.formBuilder.nonNullable.control('', [Validators.required]),
    email: new FormControl<string|null>(null, [Validators.email]),
    phone: this.formBuilder.group({
      phoneNumber: this.formBuilder.nonNullable.control(''),
      country: this.formBuilder.nonNullable.control(COUNTRIES_WITH_CALLING_CODES[0].code),
    }),
  })

  constructor() {
  }

  phoneFormGroup() {
    return this.addFriendForm.get('phone') as FormGroup;
  }

  ngOnInit() {
    if (this.isUpdating()) {
      this.addFriendForm.patchValue({
        name: this.friend()?.name,
        email: this.friend()?.email,
        phone: {
          phoneNumber: toNationalPhone(this.friend()?.phone!),
          country: extractCountryCode(this.friend()?.phone!) || COUNTRIES_WITH_CALLING_CODES[0].code
        }
      })
    } else {
      this.addFriendForm.patchValue({
        name: this.name,
      })
    }
  }

  cancel() {
    this.modalCtrl.dismiss(null, 'cancel');
  }

  async onSubmit() {
    if (this.addFriendForm.valid) {
      try {
        this.loading.set(true);
        const mappedValue = {
          name: this.addFriendForm.get('name')!.value,
          email: this.addFriendForm.get('email')!.value || null,
          phoneNumber: toInternationalPhone(this.addFriendForm.get('phone.phoneNumber')!.value, this.addFriendForm.get('phone.country')!.value),
        }
        const friend = this.isUpdating() ?
          await this.friendsStore.updateFriend(this.friend()!.friendId, mappedValue) :
          await this.friendsStore.addFriend(mappedValue);
        await this.modalCtrl.dismiss(friend, 'confirm')
      } finally {
        this.loading.set(false);
      }
    } else {
      this.addFriendForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }
}
