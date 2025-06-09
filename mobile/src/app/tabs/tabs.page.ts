import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { IonTabs, IonTabBar, IonTabButton, IonIcon } from '@ionic/angular/standalone';
import { Router } from '@angular/router';
import { FriendsStore } from '../friends/friends.store';
import { DefineExpenseService } from '../define-expense/define-expense.service';

@Component({
  selector: 'mr-tabs',
  templateUrl: 'tabs.page.html',
  styleUrls: ['tabs.page.scss'],
  standalone: true,
  imports: [IonTabs, IonTabBar, IonTabButton, IonIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabsPage {
  readonly router = inject(Router);
  readonly friendsStore = inject(FriendsStore);
  readonly defineExpenseService = inject(DefineExpenseService);
  constructor() {
  }

  async addNewExpense() {
    this.defineExpenseService.openDefineExpenseModal();
  }
}
