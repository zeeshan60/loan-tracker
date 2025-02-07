import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { ModalController } from '@ionic/angular/standalone';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { FriendsService } from '../friends/friends.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-add-friend',
  templateUrl: './add-friend.component.html',
  styleUrls: ['./add-friend.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonicModule,
    FormsModule,
    NgIf,
  ],
})
export class AddFriendComponent  implements OnInit {
  name = '';
  readonly loading = signal(false);
  readonly friendsService = inject(FriendsService);
  constructor(private modalCtrl: ModalController) { }

  ngOnInit() {}

  cancel() {
    this.modalCtrl.dismiss(null, 'cancel');
  }
  async confirm() {
    this.loading.set(true);
    const friend = await firstValueFrom(this.friendsService.createFriend({ name: 'somewhat' }));
    this.loading.set(false);
    this.modalCtrl.dismiss(friend, 'confirm')
  }
}
