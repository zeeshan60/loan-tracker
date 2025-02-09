import { ChangeDetectionStrategy, Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import {
  IonButton,
  IonButtons, IonContent,
  IonHeader,
  IonInput, IonItem, IonSpinner,
  IonTitle,
  IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { FriendsService } from '../friends/friends.service';
import { firstValueFrom } from 'rxjs';
import { ToastController } from '@ionic/angular';

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
    IonItem,
    ReactiveFormsModule,
  ],
})
export class AddFriendComponent  implements OnInit {
  name = '';
  readonly loading = signal(false);
  readonly friendsService = inject(FriendsService);
  private formBuilder = inject(FormBuilder);
  private toastCtrl = inject(ToastController);
  public addFriendForm = this.formBuilder.group({
    name: ['', Validators.required],
    email: ['', Validators.email],
    phone: ['', Validators.required],
  })

  constructor(private modalCtrl: ModalController) { }

  ngOnInit() {}

  cancel() {
    this.modalCtrl.dismiss(null, 'cancel');
  }

  async onSubmit($event: any) {
    if (this.addFriendForm.valid) {
      this.loading.set(true);
      const friend = await firstValueFrom(this.friendsService.createFriend({ name: 'somewhat' }));
      this.loading.set(false);
      this.modalCtrl.dismiss(friend, 'confirm')
    } else {
      this.addFriendForm.markAllAsTouched();
      const toast = await this.toastCtrl.create({
        message: 'Please fill in the correct values',
        duration: 1500
      });
      toast.present();
    }
  }
}
