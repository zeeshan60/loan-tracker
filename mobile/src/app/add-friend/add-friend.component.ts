import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Input,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import {
  IonButton,
  IonButtons, IonContent,
  IonHeader,
  IonInput, IonSpinner,
  IonTitle,
  IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { FriendsService } from '../friends/friends.service';
import { firstValueFrom } from 'rxjs';
import { ToastController } from '@ionic/angular';
import { DEFAULT_TOAST_DURATION } from '../constants';
import { CreateFriend } from './model';
import { HelperService } from '../helper.service';
import { FriendsStore } from '../friends/friends.store';

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
  ],
})
export class AddFriendComponent implements OnInit {
  @Input() name: string = '';
  readonly friendsStore = inject(FriendsStore);
  private formBuilder = inject(FormBuilder);
  private helperService = inject(HelperService);

  readonly loading = signal(false);
  public addFriendForm = this.formBuilder.group({
    name: this.formBuilder.nonNullable.control('', [Validators.required]),
    email: ['', Validators.email],
    phoneNumber: this.formBuilder.nonNullable.control('', [Validators.required]),
  })

  constructor(private modalCtrl: ModalController) {
  }

  ngOnInit() {
    // set the passed name
    this.addFriendForm.patchValue({
      name: this.name,
    })
  }

  cancel() {
    this.modalCtrl.dismiss(null, 'cancel');
  }

  async onSubmit() {
    if (this.addFriendForm.valid) {
      try {
        this.loading.set(true);
        const friend = await this.friendsStore.addFriend(this.addFriendForm.getRawValue());
        await this.modalCtrl.dismiss(friend, 'confirm')
      } catch (e) {
        await this.helperService.showToast('Unable to add friend at the moment');
      } finally {
        this.loading.set(false);
      }
    } else {
      this.addFriendForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }
}
