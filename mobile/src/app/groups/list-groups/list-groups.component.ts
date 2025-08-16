import { Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  IonAvatar,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel, IonList, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import { GroupsStore } from '../groups.store';
import { GroupWithBalance } from '../model';

@Component({
  selector: 'mr-list-groups',
  templateUrl: './list-groups.component.html',
  styleUrls: ['./list-groups.component.scss'],
  imports: [
    CurrencyPipe,
    IonAvatar,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonItem,
    IonLabel,
    IonList,
    IonTitle,
    IonToolbar,
  ],
})
export class ListGroupsComponent {
  groupsStore = inject(GroupsStore);
  constructor() { }

  async addGroup() {

  }

  async chooseGroup(group: GroupWithBalance) {

  }
}
