from collections import namedtuple
import pandas as pd
import numpy as np

Branch = namedtuple("Branch", "country state city population")

branches = pd.DataFrame([
    Branch("USA", "New York", "New York", 8804190),
    Branch("USA", "California", "Los Angeles", 3898747),
    Branch("USA", "Illinois", "Chicago", 2746388),
    Branch("USA", "Texas", "Houston", 2304580),
    Branch("USA", "Arizona", "Phoenix", 1608139),
    Branch("USA", "Pennsylvania", "Philadelphia", 1603797),
    Branch("USA", "Texas", "San Antonio", 1434625),
    Branch("USA", "California", "San Diego", 1386932),
    Branch("USA", "Texas", "Dallas", 1304379),
    Branch("USA", "California", "San Jose", 1013240),
    Branch("Germany", "Berlin", "Berlin", 3520031),
    Branch("Germany", "Hamburg", "Hamburg", 1787408),
    Branch("Germany", "Bavaria", "Munich", 1450381),
    Branch("Germany", "North Rhine-Westphalia", "Cologne", 1060582),
    Branch("Germany", "Hesse", "Frankfurt Am Main", 732688),
    Branch("Spain", "Madrid", "Madrid", 6155116),
    Branch("Spain", "Barcelona", "Barcelona", 5179243),
    Branch("Sweden", "Stockholm", "Stockholm", 975000),
    Branch("Sweden", "Västra Götaland", "Gothenburg", 570000),
    Branch("Sweden", "Skåne", "Malmö", 350647)
])

branches.index += 1

head_branches = branches[branches.groupby('country').population.transform(max) == branches.population]

total_population = branches.population.sum()

if __name__ == '__main__':
    engineers_per_branch = (branches.population / total_population * 10000).to_numpy()
    bms_per_branch = np.maximum(engineers_per_branch / 200, 1).round().astype(int)
    engineers_per_branch = engineers_per_branch.astype(int)
    
    print(engineers_per_branch)