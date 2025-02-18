import { Component } from '@angular/core';
import { IonNav } from '@ionic/angular/standalone';
import { SelectFriendComponent } from './select-friend/select-friend.component';

@Component({
  selector: 'app-add-new',
  templateUrl: 'add-new.page.html',
  styleUrls: ['add-new.page.scss'],
  standalone: true,
  imports: [IonNav],
})
export class AddNewPage {
  component = SelectFriendComponent;
  constructor() {}

}
