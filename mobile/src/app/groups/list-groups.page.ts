import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IonNav } from '@ionic/angular/standalone';

@Component({
  selector: 'mr-list-groups',
  templateUrl: 'list-groups.page.html',
  styleUrls: ['list-groups.page.scss'],
  standalone: true,
  imports: [
    IonNav
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListGroupsPage {
  constructor() {}
}
