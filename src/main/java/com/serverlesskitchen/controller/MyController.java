package com.serverlesskitchen.controller;

import com.serverlesskitchen.model.Ingredients;
import com.serverlesskitchen.model.Inventory;
import com.serverlesskitchen.model.Recipe;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.serverlesskitchen.model.RecipeCount;
import com.serverlesskitchen.repository.InventoryRepository;
import com.serverlesskitchen.repository.KitchenRepository;
import com.serverlesskitchen.repository.SequenceRepository;
import com.serverlesskitchen.service.NextSequenceService;
import com.serverlesskitchen.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@Component
public class MyController {

    public boolean status = true;
    @Autowired
    private KitchenRepository repository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private NextSequenceService nextSequenceService;
    @Autowired
    private SequenceRepository sequenceRepository;
    @Autowired
    private RecipeService recipeService;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/recipes/create")
    public String saveRecipe(@RequestBody Recipe recipe) {
        recipe.setKitchenId(nextSequenceService.getNextSequence("CustomSequences"));
        repository.save(recipe);
        return "Added recipe with ID: " + recipe.getKitchenId();
    }

    @GetMapping("/recipes")
    public List<Recipe> getRecipe() {
        return repository.findAll();
    }

    @GetMapping("/recipes/{id}")
    public Optional<Recipe> getRecipeById(@PathVariable int id) {
        return repository.findById(id);
    }

    @DeleteMapping("/clear")
    public String deleteAllRecipe() {
        repository.deleteAll();
        inventoryRepository.deleteAll();
        sequenceRepository.deleteAll();
        return "All Recipes and inventory cleared";
    }

    @DeleteMapping("/recipes/{id}")
    public String deleteRecipe(@PathVariable int id) {
        repository.deleteById(id);
        return "Recipe delete with id: " + id;
    }

    @GetMapping("/inventory")
    public List<Inventory> getInventoryList() {
        return inventoryRepository.findAll();
    }

    @PostMapping("/inventory/fill")
    public String fillInventory(@RequestBody Inventory inventory) {
        int requestedQuantity = inventory.getQuantity();
        int existingQuantity;
        int finalQuantity = 0;
        if (inventory.getQuantity() > 0) {
            existingQuantity = findExistingQuantity(inventory.getName());
            if (status == true) {
                finalQuantity = existingQuantity + requestedQuantity;
                inventory.setQuantity(finalQuantity);
            } else {
                finalQuantity = inventory.getQuantity();
                inventory.setQuantity(inventory.getQuantity());
            }
            inventoryRepository.save(inventory);
            return "Inventory filled with " + inventory.getName() + "Quantity :" + finalQuantity;
        } else {
            return "Invalid Quantity";
        }
    }

    public int findExistingQuantity(String name) {
        try {
            Optional<Inventory> existingQuantity = inventoryRepository.findById(name);
            int quantity = existingQuantity.get().getQuantity();
            status = true;
            return quantity;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            status = false;
            return 0;
        }
    }

    @PatchMapping("/recipes/{id}")
    public ResponseEntity<String> updateExistingRecipe(@RequestBody Recipe recipe, @PathVariable("id") int id) {
        recipeService.update(id, recipe.getName());
        return ResponseEntity.ok("updated");

    }

    @GetMapping("/recipes/get-count-by-recipe")
    public List<RecipeCount> getCountByRecipe() {
        int counter;
        int reduceQuantity;
        int finalCounter = 0;
        RecipeCount recipeCounter = new RecipeCount();
        List<Recipe> fetchAllRecipe = repository.findAll();
        List<RecipeCount> recipeMessage = new ArrayList<>();
        for (Recipe i : fetchAllRecipe) {
            recipeCounter.setId(i.getKitchenId());
            for (Ingredients ingredients : i.getIngredients()
            ) {
                counter = 0;
                List<Inventory> inventoryList = inventoryRepository.findAll();
                for (Inventory j : inventoryList
                ) {
                    reduceQuantity = j.getQuantity();
                    while ((ingredients.getName().equals(j.getName())) && (reduceQuantity >= ingredients.getQuantity())) {
                        reduceQuantity = j.getQuantity() - ingredients.getQuantity();
                        j.setQuantity(reduceQuantity);
                        counter++;
                        System.out.println("Counter: " + counter);
                    }
                    finalCounter = counter;
                    System.out.println("Ingredient Name: " + ingredients.getName());
                    System.out.println("Final Counter: " + finalCounter);
                }
                if (finalCounter > counter) {
                    recipeCounter.setCount(counter);
                } else {
                    recipeCounter.setCount(finalCounter);
                }
            }
            recipeMessage.add(recipeCounter);
        }
        return recipeMessage;
    }
}