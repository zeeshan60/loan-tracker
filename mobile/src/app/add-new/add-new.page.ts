import { Component } from '@angular/core';
import { IonHeader, IonToolbar, IonTitle, IonContent, IonNav, ModalController } from '@ionic/angular/standalone';
import { SelectFriendComponent } from './select-friend/select-friend.component';

@Component({
  selector: 'app-add-new',
  templateUrl: 'add-new.page.html',
  styleUrls: ['add-new.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonNav],
})
export class AddNewPage {
  component = SelectFriendComponent;
  constructor() {}

}
