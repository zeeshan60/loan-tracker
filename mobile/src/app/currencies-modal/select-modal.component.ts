import { ChangeDetectionStrategy, Component, computed, inject, input, model, OnInit } from '@angular/core';
import {
  IonContent, IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonSearchbar,
} from '@ionic/angular/standalone';
import { ModalIndex, ModalService } from '../modal.service';
import { FormsModule } from '@angular/forms';

type Item = {
  optionLabel: string, [key: string]: any
};
@Component({
  selector: 'mr-select-modal',
  templateUrl: './select-modal.component.html',
  styleUrls: ['./select-modal.component.scss'],
  standalone: true,
  imports: [
    IonContent,
    IonSearchbar,
    IonList,
    IonItem,
    IonLabel,
    FormsModule,
    IonIcon,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectModalComponent {
  modalIndex = input.required<ModalIndex>()
  items = input.required<Item[]>();
  selectedItem = input<Item|null>();
  mostlyUsedItems = input<Item[]>();
  filter = model<string>('');
  private filteredItems = computed(() => {
    return this.items()
      .filter((item) => item.optionLabel.toLowerCase()
        .includes(this.filter().toLowerCase()))
  });
  filteredMostlyUsedItems = computed(() => {
    return this.mostlyUsedItems()
      .filter((item) => item.optionLabel.toLowerCase()
        .includes(this.filter().toLowerCase()))
  });
  unUsedCurrencies = computed(() => {
    const usedCurrencies = this.mostlyUsedItems().map(item => item.optionLabel)
    return this.filteredItems().filter(item => !usedCurrencies.includes(item.optionLabel))
  });
  modalService = inject(ModalService);
  constructor() { }

  selectItem(item: Item) {
    this.modalService.dismiss(this.modalIndex(), item, 'confirm');
  }
}
