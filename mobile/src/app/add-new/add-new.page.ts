import { Component, Input } from '@angular/core';
import { IonNav } from '@ionic/angular/standalone';
import { SelectFriendComponent } from './select-friend-nav/select-friend.component';
import { DefineExpenseComponent } from './define-expense/define-expense.component';
import { Friend } from '../friends/model';

@Component({
  selector: 'app-add-new',
  templateUrl: 'add-new.page.html',
  styleUrls: ['add-new.page.scss'],
  standalone: true,
  imports: [IonNav],
})
export class AddNewPage {
  selectFriendComponent = SelectFriendComponent;
  defineExpenseComponent = DefineExpenseComponent;
  @Input() selectedFriend: Friend | null = null;
  constructor() {}
}
