import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IonNav } from '@ionic/angular/standalone';
import { ListGroupsComponent } from './list-groups/list-groups.component';

@Component({
  selector: 'mr-groups',
  templateUrl: 'groups.page.html',
  styleUrls: ['groups.page.scss'],
  standalone: true,
  imports: [
    IonNav
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsPage {
  listGroupsComponent = ListGroupsComponent;
  constructor() {}
}
