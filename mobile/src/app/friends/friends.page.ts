import { ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ListFriendsComponent } from './list-friends/list-friends.component';
import { IonNav } from '@ionic/angular/standalone';

@Component({
  selector: 'app-friends',
  templateUrl: 'friends.page.html',
  styleUrls: ['friends.page.scss'],
  standalone: true,
  imports: [
    IonNav
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FriendsPage {
  readonly listFriendsComponent = ListFriendsComponent;
  constructor() {}
}
