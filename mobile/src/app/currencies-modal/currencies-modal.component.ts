import { ChangeDetectionStrategy, Component, inject, input, OnInit } from '@angular/core';
import {
  IonAvatar,
  IonContent,
  IonImg,
  IonItem,
  IonLabel,
  IonList,
  IonSearchbar,
} from '@ionic/angular/standalone';
import { ModalIndex, ModalService } from '../modal.service';

@Component({
  selector: 'app-currencies-modal',
  templateUrl: './currencies-modal.component.html',
  styleUrls: ['./currencies-modal.component.scss'],
  standalone: true,
  imports: [
    IonContent,
    IonSearchbar,
    IonList,
    IonItem,
    IonAvatar,
    IonImg,
    IonLabel,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CurrenciesModalComponent  implements OnInit {
  modalIndex = input.required<ModalIndex>()
  modalService = inject(ModalService);
  constructor() { }

  ngOnInit() {
  }

}
